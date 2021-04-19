/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner;

import static com.google.cloud.spanner.SpannerExceptionFactory.newSpannerException;
import static com.google.cloud.spanner.SpannerExceptionFactory.newSpannerExceptionForCancellation;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.cloud.spanner.v1.stub.SpannerStubSettings;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.ByteString;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value.KindCase;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.ResultSetStats;
import com.google.spanner.v1.Transaction;
import com.google.spanner.v1.TypeCode;
import io.grpc.Context;
import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** Implementation of {@link ResultSet}. */
abstract class AbstractResultSet<R> extends AbstractStructReader implements ResultSet {
  private static final Tracer tracer = Tracing.getTracer();

  interface Listener {
    /**
     * Called when transaction metadata is seen. This method may be invoked at most once. If the
     * method is invoked, it will precede {@link #onError(SpannerException)} or {@link #onDone()}.
     */
    void onTransactionMetadata(Transaction transaction, boolean shouldIncludeId)
        throws SpannerException;

    /** Called when the read finishes with an error. Returns the error that should be thrown. */
    SpannerException onError(SpannerException e, boolean withBeginTransaction);

    /** Called when the read finishes normally. */
    void onDone(boolean withBeginTransaction);
  }

  @VisibleForTesting
  static class GrpcResultSet extends AbstractResultSet<List<Object>> {
    private final GrpcValueIterator iterator;
    private final Listener listener;
    private GrpcStruct currRow;
    private SpannerException error;
    private ResultSetStats statistics;
    private boolean closed;

    GrpcResultSet(CloseableIterator<PartialResultSet> iterator, Listener listener) {
      this.iterator = new GrpcValueIterator(iterator);
      this.listener = listener;
    }

    @Override
    protected GrpcStruct currRow() {
      checkState(!closed, "ResultSet is closed");
      checkState(currRow != null, "next() call required");
      return currRow;
    }

    @Override
    public boolean next() throws SpannerException {
      if (error != null) {
        throw newSpannerException(error);
      }
      try {
        if (currRow == null) {
          ResultSetMetadata metadata = iterator.getMetadata();
          if (metadata.hasTransaction()) {
            listener.onTransactionMetadata(
                metadata.getTransaction(), iterator.isWithBeginTransaction());
          } else if (iterator.isWithBeginTransaction()) {
            // The query should have returned a transaction.
            throw SpannerExceptionFactory.newSpannerException(
                ErrorCode.FAILED_PRECONDITION, AbstractReadContext.NO_TRANSACTION_RETURNED_MSG);
          }
          currRow = new GrpcStruct(iterator.type(), new ArrayList<>());
        }
        boolean hasNext = currRow.consumeRow(iterator);
        if (!hasNext) {
          statistics = iterator.getStats();
        }
        return hasNext;
      } catch (Throwable t) {
        throw yieldError(
            SpannerExceptionFactory.asSpannerException(t),
            iterator.isWithBeginTransaction() && currRow == null);
      }
    }

    @Override
    @Nullable
    public ResultSetStats getStats() {
      return statistics;
    }

    @Override
    public void close() {
      listener.onDone(iterator.isWithBeginTransaction());
      iterator.close("ResultSet closed");
      closed = true;
    }

    @Override
    public Type getType() {
      checkState(currRow != null, "next() call required");
      return currRow.getType();
    }

    private SpannerException yieldError(SpannerException e, boolean beginTransaction) {
      SpannerException toThrow = listener.onError(e, beginTransaction);
      close();
      throw toThrow;
    }
  }
  /**
   * Adapts a stream of {@code PartialResultSet} messages into a stream of {@code Value} messages.
   */
  private static class GrpcValueIterator extends AbstractIterator<com.google.protobuf.Value> {
    private enum StreamValue {
      METADATA,
      RESULT,
    }

    private final CloseableIterator<PartialResultSet> stream;
    private ResultSetMetadata metadata;
    private Type type;
    private PartialResultSet current;
    private int pos;
    private ResultSetStats statistics;

    GrpcValueIterator(CloseableIterator<PartialResultSet> stream) {
      this.stream = stream;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected com.google.protobuf.Value computeNext() {
      if (!ensureReady(StreamValue.RESULT)) {
        endOfData();
        return null;
      }
      com.google.protobuf.Value value = current.getValues(pos++);
      KindCase kind = value.getKindCase();

      if (!isMergeable(kind)) {
        if (pos == current.getValuesCount() && current.getChunkedValue()) {
          throw newSpannerException(ErrorCode.INTERNAL, "Unexpected chunked PartialResultSet.");
        } else {
          return value;
        }
      }
      if (!current.getChunkedValue() || pos != current.getValuesCount()) {
        return value;
      }

      Object merged =
          kind == KindCase.STRING_VALUE
              ? value.getStringValue()
              : new ArrayList<>(value.getListValue().getValuesList());
      while (current.getChunkedValue() && pos == current.getValuesCount()) {
        if (!ensureReady(StreamValue.RESULT)) {
          throw newSpannerException(
              ErrorCode.INTERNAL, "Stream closed in the middle of chunked value");
        }
        com.google.protobuf.Value newValue = current.getValues(pos++);
        if (newValue.getKindCase() != kind) {
          throw newSpannerException(
              ErrorCode.INTERNAL,
              "Unexpected type in middle of chunked value. Expected: "
                  + kind
                  + " but got: "
                  + newValue.getKindCase());
        }
        if (kind == KindCase.STRING_VALUE) {
          merged = (String) merged + newValue.getStringValue();
        } else {
          concatLists(
              (List<com.google.protobuf.Value>) merged, newValue.getListValue().getValuesList());
        }
      }
      if (kind == KindCase.STRING_VALUE) {
        return com.google.protobuf.Value.newBuilder().setStringValue((String) merged).build();
      } else {
        return com.google.protobuf.Value.newBuilder()
            .setListValue(
                ListValue.newBuilder().addAllValues((List<com.google.protobuf.Value>) merged))
            .build();
      }
    }

    ResultSetMetadata getMetadata() throws SpannerException {
      if (metadata == null) {
        if (!ensureReady(StreamValue.METADATA)) {
          throw newSpannerException(ErrorCode.INTERNAL, "Stream closed without sending metadata");
        }
      }
      return metadata;
    }

    /**
     * Get the query statistics. Query statistics are delivered with the last PartialResultSet in
     * the stream. Any attempt to call this method before the caller has finished consuming the
     * results will return null.
     */
    @Nullable
    ResultSetStats getStats() {
      return statistics;
    }

    Type type() {
      checkState(type != null, "metadata has not been received");
      return type;
    }

    private boolean ensureReady(StreamValue requiredValue) throws SpannerException {
      while (current == null || pos >= current.getValuesCount()) {
        if (!stream.hasNext()) {
          return false;
        }
        current = stream.next();
        pos = 0;
        if (type == null) {
          // This is the first message on the stream.
          if (!current.hasMetadata() || !current.getMetadata().hasRowType()) {
            throw newSpannerException(ErrorCode.INTERNAL, "Missing type metadata in first message");
          }
          metadata = current.getMetadata();
          com.google.spanner.v1.Type typeProto =
              com.google.spanner.v1.Type.newBuilder()
                  .setCode(TypeCode.STRUCT)
                  .setStructType(metadata.getRowType())
                  .build();
          try {
            type = Type.fromProto(typeProto);
          } catch (IllegalArgumentException e) {
            throw newSpannerException(
                ErrorCode.INTERNAL, "Invalid type metadata: " + e.getMessage(), e);
          }
        }
        if (current.hasStats()) {
          statistics = current.getStats();
        }
        if (requiredValue == StreamValue.METADATA) {
          return true;
        }
      }
      return true;
    }

    void close(@Nullable String message) {
      stream.close(message);
    }

    boolean isWithBeginTransaction() {
      return stream.isWithBeginTransaction();
    }

    /** @param a is a mutable list and b will be concatenated into a. */
    private void concatLists(List<com.google.protobuf.Value> a, List<com.google.protobuf.Value> b) {
      if (a.size() == 0 || b.size() == 0) {
        a.addAll(b);
        return;
      } else {
        com.google.protobuf.Value last = a.get(a.size() - 1);
        com.google.protobuf.Value first = b.get(0);
        KindCase lastKind = last.getKindCase();
        KindCase firstKind = first.getKindCase();
        if (isMergeable(lastKind) && lastKind == firstKind) {
          com.google.protobuf.Value merged = null;
          if (lastKind == KindCase.STRING_VALUE) {
            String lastStr = last.getStringValue();
            String firstStr = first.getStringValue();
            merged =
                com.google.protobuf.Value.newBuilder().setStringValue(lastStr + firstStr).build();
          } else { // List
            List<com.google.protobuf.Value> mergedList = new ArrayList<>();
            mergedList.addAll(last.getListValue().getValuesList());
            concatLists(mergedList, first.getListValue().getValuesList());
            merged =
                com.google.protobuf.Value.newBuilder()
                    .setListValue(ListValue.newBuilder().addAllValues(mergedList))
                    .build();
          }
          a.set(a.size() - 1, merged);
          a.addAll(b.subList(1, b.size()));
        } else {
          a.addAll(b);
        }
      }
    }

    private boolean isMergeable(KindCase kind) {
      return kind == KindCase.STRING_VALUE || kind == KindCase.LIST_VALUE;
    }
  }

  static class GrpcStruct extends Struct implements Serializable {
    private final Type type;
    private final List<Value> rowData;

    /**
     * Builds an immutable version of this struct using {@link Struct#newBuilder()} which is used as
     * a serialization proxy.
     */
    private Object writeReplace() {
      Builder builder = Struct.newBuilder();
      List<Type.StructField> structFields = getType().getStructFields();
      for (int i = 0; i < structFields.size(); i++) {
        Type.StructField field = structFields.get(i);
        String fieldName = field.getName();
        Value value = rowData.get(i);
        Type fieldType = field.getType();
        switch (fieldType.getCode()) {
          case BOOL:
            builder.set(fieldName).to(value == null ? null : value.getBool());
            break;
          case INT64:
            builder.set(fieldName).to(value == null ? null : value.getInt64());
            break;
          case FLOAT64:
            builder.set(fieldName).to(value == null ? null : value.getFloat64());
            break;
          case NUMERIC:
            builder.set(fieldName).to(value == null ? null : value.getNumeric());
            break;
          case STRING:
            builder.set(fieldName).to(value == null ? null : value.getString());
            break;
          case BYTES:
            builder.set(fieldName).to(value == null ? null : value.getBytes());
            break;
          case TIMESTAMP:
            builder.set(fieldName).to(value == null ? null : value.getTimestamp());
            break;
          case DATE:
            builder.set(fieldName).to(value == null ? null : value.getDate());
            break;
          case ARRAY:
            switch (fieldType.getArrayElementType().getCode()) {
              case BOOL:
                builder.set(fieldName).toBoolArray(value == null ? null : value.getBoolArray());
                break;
              case INT64:
                builder.set(fieldName).toInt64Array(value == null ? null : value.getInt64Array());
                break;
              case FLOAT64:
                builder
                    .set(fieldName)
                    .toFloat64Array(value == null ? null : value.getFloat64Array());
                break;
              case NUMERIC:
                builder
                    .set(fieldName)
                    .toNumericArray(value == null ? null : value.getNumericArray());
                break;
              case STRING:
                builder.set(fieldName).toStringArray(value == null ? null : value.getStringArray());
                break;
              case BYTES:
                builder.set(fieldName).toBytesArray(value == null ? null : value.getBytesArray());
                break;
              case TIMESTAMP:
                builder
                    .set(fieldName)
                    .toTimestampArray(value == null ? null : value.getTimestampArray());
                break;
              case DATE:
                builder.set(fieldName).toDateArray(value == null ? null : value.getDateArray());
                break;
              case STRUCT:
                builder
                    .set(fieldName)
                    .toStructArray(
                        fieldType.getArrayElementType(),
                        value == null ? null : value.getStructArray());
                break;
              default:
                throw new AssertionError(
                    "Unhandled array type code: " + fieldType.getArrayElementType());
            }
            break;
          case STRUCT:
            if (value == null) {
              builder.set(fieldName).to(fieldType, null);
            } else {
              builder.set(fieldName).to(value.getStruct());
            }
            break;
          default:
            throw new AssertionError("Unhandled type code: " + fieldType.getCode());
        }
      }
      return builder.build();
    }

    GrpcStruct(Type type, List<Value> rowData) {
      this.type = type;
      this.rowData = rowData;
    }

    @Override
    public String toString() {
      return this.rowData.toString();
    }

    boolean consumeRow(Iterator<com.google.protobuf.Value> iterator) {
      rowData.clear();
      if (!iterator.hasNext()) {
        return false;
      }
      for (Type.StructField fieldType : getType().getStructFields()) {
        if (!iterator.hasNext()) {
          throw newSpannerException(
              ErrorCode.INTERNAL,
              "Invalid value stream: end of stream reached before row is complete");
        }
        com.google.protobuf.Value value = iterator.next();
        rowData.add(decodeValue(fieldType.getType(), value));
      }
      return true;
    }

    private static Value decodeValue(Type fieldType, com.google.protobuf.Value proto) {
      if (proto.getKindCase() == KindCase.NULL_VALUE) {
        return null;
      }
      switch (fieldType.getCode()) {
        case BOOL:
          checkType(fieldType, proto, KindCase.BOOL_VALUE);
          return Value.bool(proto.getBoolValue());
        case INT64:
          checkType(fieldType, proto, KindCase.STRING_VALUE);
          return Value.int64(Long.parseLong(proto.getStringValue()));
        case FLOAT64:
          return Value.float64(valueProtoToFloat64(proto));
        case NUMERIC:
          return Value.numeric(new BigDecimal(proto.getStringValue()));
        case STRING:
          checkType(fieldType, proto, KindCase.STRING_VALUE);
          return Value.string(proto.getStringValue());
        case BYTES:
          checkType(fieldType, proto, KindCase.STRING_VALUE);
          return Value.bytes(ByteArray.fromBase64(proto.getStringValue()));
        case TIMESTAMP:
          checkType(fieldType, proto, KindCase.STRING_VALUE);
          return Value.timestamp(Timestamp.parseTimestamp(proto.getStringValue()));
        case DATE:
          checkType(fieldType, proto, KindCase.STRING_VALUE);
          return Value.date(Date.parseDate(proto.getStringValue()));
        case ARRAY:
          checkType(fieldType, proto, KindCase.LIST_VALUE);
          ListValue listValue = proto.getListValue();
          return decodeArrayValue(fieldType.getArrayElementType(), listValue);
        case STRUCT:
          checkType(fieldType, proto, KindCase.LIST_VALUE);
          ListValue structValue = proto.getListValue();
          return Value.struct(decodeStructValue(fieldType, structValue));
        default:
          throw new AssertionError("Unhandled type code: " + fieldType.getCode());
      }
    }

    private static Struct decodeStructValue(Type structType, ListValue structValue) {
      List<Type.StructField> fieldTypes = structType.getStructFields();
      checkArgument(
          structValue.getValuesCount() == fieldTypes.size(),
          "Size mismatch between type descriptor and actual values.");
      List<Value> fields = new ArrayList<>(fieldTypes.size());
      List<com.google.protobuf.Value> fieldValues = structValue.getValuesList();
      for (int i = 0; i < fieldTypes.size(); ++i) {
        fields.add(decodeValue(fieldTypes.get(i).getType(), fieldValues.get(i)));
      }
      return new GrpcStruct(structType, fields);
    }

    static Value decodeArrayValue(Type elementType, ListValue listValue) {
      switch (elementType.getCode()) {
        case BOOL:
          return Value.boolArray(
              decodeListValue(listValue, com.google.protobuf.Value::getBoolValue));
        case INT64:
          return Value.int64Array(
              decodeListValue(listValue, value -> Long.parseLong(value.getStringValue())));
        case FLOAT64:
          return Value.float64Array(
              decodeListValue(listValue, AbstractResultSet::valueProtoToFloat64));
        case NUMERIC:
          return Value.numericArray(
              decodeListValue(listValue, value -> new BigDecimal(value.getStringValue())));
        case STRING:
          return Value.stringArray(
              decodeListValue(listValue, com.google.protobuf.Value::getStringValue));
        case BYTES:
          return Value.bytesArray(
              decodeListValue(listValue, value -> ByteArray.fromBase64(value.getStringValue())));
        case TIMESTAMP:
          return Value.timestampArray(
              decodeListValue(
                  listValue, value -> Timestamp.parseTimestamp(value.getStringValue())));
        case DATE:
          return Value.dateArray(
              decodeListValue(listValue, value -> Date.parseDate(value.getStringValue())));
        case STRUCT:
          return Value.structArray(
              elementType,
              decodeListValue(
                  listValue, value -> decodeStructValue(elementType, value.getListValue())));
        default:
          throw new AssertionError("Unhandled type code: " + elementType.getCode());
      }
    }

    private static <T> List<T> decodeListValue(
        ListValue listValue, Function<com.google.protobuf.Value, T> valueDecoder) {
      return listValue.getValuesList().stream()
          .map(
              value ->
                  value.getKindCase() == KindCase.NULL_VALUE ? null : valueDecoder.apply(value))
          .collect(Collectors.toList());
    }

    private static void checkType(
        Type fieldType, com.google.protobuf.Value proto, KindCase expected) {
      if (proto.getKindCase() != expected) {
        throw newSpannerException(
            ErrorCode.INTERNAL,
            "Invalid value for column type "
                + fieldType
                + " expected "
                + expected
                + " but was "
                + proto.getKindCase());
      }
    }

    Struct immutableCopy() {
      return new GrpcStruct(type, new ArrayList<>(rowData));
    }

    @Override
    public Type getType() {
      return type;
    }

    @Override
    public boolean isNull(int columnIndex) {
      return rowData.get(columnIndex) == null;
    }

    @Override
    protected boolean getBooleanInternal(int columnIndex) {
      return rowData.get(columnIndex).getBool();
    }

    @Override
    protected long getLongInternal(int columnIndex) {
      return rowData.get(columnIndex).getInt64();
    }

    @Override
    protected double getDoubleInternal(int columnIndex) {
      return rowData.get(columnIndex).getFloat64();
    }

    @Override
    protected BigDecimal getBigDecimalInternal(int columnIndex) {
      return rowData.get(columnIndex).getNumeric();
    }

    @Override
    protected String getStringInternal(int columnIndex) {
      return rowData.get(columnIndex).getString();
    }

    @Override
    protected ByteArray getBytesInternal(int columnIndex) {
      return rowData.get(columnIndex).getBytes();
    }

    @Override
    protected Timestamp getTimestampInternal(int columnIndex) {
      return rowData.get(columnIndex).getTimestamp();
    }

    @Override
    protected Date getDateInternal(int columnIndex) {
      return rowData.get(columnIndex).getDate();
    }

    @Override
    protected Value getValueInternal(int columnIndex) {
      return rowData.get(columnIndex);
    }

    @Override
    protected Struct getStructInternal(int columnIndex) {
      return rowData.get(columnIndex).getStruct();
    }

    @Override
    protected boolean[] getBooleanArrayInternal(int columnIndex) {
      final List<Boolean> values = rowData.get(columnIndex).getBoolArray();
      boolean[] r = new boolean[values.size()];
      for (int i = 0; i < values.size(); ++i) {
        if (values.get(i) == null) {
          throw throwNotNull(columnIndex);
        }
        r[i] = values.get(i);
      }
      return r;
    }

    @Override
    protected List<Boolean> getBooleanListInternal(int columnIndex) {
      return Collections.unmodifiableList(rowData.get(columnIndex).getBoolArray());
    }

    @Override
    protected long[] getLongArrayInternal(int columnIndex) {
      final List<Long> values = rowData.get(columnIndex).getInt64Array();
      int i = 0;
      long[] result = new long[values.size()];
      for (Long value : values) {
        result[i++] = value;
      }
      return result;
    }

    @Override
    protected List<Long> getLongListInternal(int columnIndex) {
      return Collections.unmodifiableList(rowData.get(columnIndex).getInt64Array());
    }

    @Override
    protected double[] getDoubleArrayInternal(int columnIndex) {
      final List<Double> values = rowData.get(columnIndex).getFloat64Array();
      int i = 0;
      double[] result = new double[values.size()];
      for (Double value : values) {
        result[i++] = value;
      }
      return result;
    }

    @Override
    protected List<Double> getDoubleListInternal(int columnIndex) {
      return Collections.unmodifiableList(rowData.get(columnIndex).getFloat64Array());
    }

    @Override
    protected List<BigDecimal> getBigDecimalListInternal(int columnIndex) {
      return Collections.unmodifiableList(rowData.get(columnIndex).getNumericArray());
    }

    @Override
    protected List<String> getStringListInternal(int columnIndex) {
      return Collections.unmodifiableList(rowData.get(columnIndex).getStringArray());
    }

    @Override
    protected List<ByteArray> getBytesListInternal(int columnIndex) {
      return Collections.unmodifiableList(rowData.get(columnIndex).getBytesArray());
    }

    @Override
    protected List<Timestamp> getTimestampListInternal(int columnIndex) {
      return Collections.unmodifiableList(rowData.get(columnIndex).getTimestampArray());
    }

    @Override
    protected List<Date> getDateListInternal(int columnIndex) {
      return Collections.unmodifiableList(rowData.get(columnIndex).getDateArray());
    }

    @Override
    protected List<Struct> getStructListInternal(int columnIndex) {
      return Collections.unmodifiableList(rowData.get(columnIndex).getStructArray());
    }
  }

  @VisibleForTesting
  interface CloseableIterator<T> extends Iterator<T> {

    /**
     * Closes the iterator, freeing any underlying resources.
     *
     * @param message a message to include in the final RPC status
     */
    void close(@Nullable String message);

    boolean isWithBeginTransaction();
  }

  /** Adapts a streaming read/query call into an iterator over partial result sets. */
  @VisibleForTesting
  static class GrpcStreamIterator extends AbstractIterator<PartialResultSet>
      implements CloseableIterator<PartialResultSet> {
    private static final Logger logger = Logger.getLogger(GrpcStreamIterator.class.getName());
    private static final PartialResultSet END_OF_STREAM = PartialResultSet.newBuilder().build();

    private final ConsumerImpl consumer = new ConsumerImpl();
    private final BlockingQueue<PartialResultSet> stream;
    private final Statement statement;

    private SpannerRpc.StreamingCall call;
    private volatile boolean withBeginTransaction;
    private SpannerException error;

    @VisibleForTesting
    GrpcStreamIterator(int prefetchChunks) {
      this(null, prefetchChunks);
    }

    @VisibleForTesting
    GrpcStreamIterator(Statement statement, int prefetchChunks) {
      this.statement = statement;
      // One extra to allow for END_OF_STREAM message.
      this.stream = new LinkedBlockingQueue<>(prefetchChunks + 1);
    }

    protected final SpannerRpc.ResultStreamConsumer consumer() {
      return consumer;
    }

    public void setCall(SpannerRpc.StreamingCall call, boolean withBeginTransaction) {
      this.call = call;
      this.withBeginTransaction = withBeginTransaction;
    }

    @Override
    public void close(@Nullable String message) {
      if (call != null) {
        call.cancel(message);
      }
    }

    @Override
    public boolean isWithBeginTransaction() {
      return withBeginTransaction;
    }

    @Override
    protected final PartialResultSet computeNext() {
      PartialResultSet next;
      try {
        // TODO: Ideally honor io.grpc.Context while blocking here.  In practice,
        //       cancellation/deadline results in an error being delivered to "stream", which
        //       should mean that we do not block significantly longer afterwards, but it would
        //       be more robust to use poll() with a timeout.
        next = stream.take();
      } catch (InterruptedException e) {
        // Treat interrupt as a request to cancel the read.
        throw SpannerExceptionFactory.propagateInterrupt(e);
      }
      if (next != END_OF_STREAM) {
        call.request(1);
        return next;
      }

      // All done - close() no longer needs to cancel the call.
      call = null;

      if (error != null) {
        throw SpannerExceptionFactory.newSpannerException(error);
      }

      endOfData();
      return null;
    }

    private void addToStream(PartialResultSet results) {
      // We assume that nothing from the user will interrupt gRPC event threads.
      Uninterruptibles.putUninterruptibly(stream, results);
    }

    private class ConsumerImpl implements SpannerRpc.ResultStreamConsumer {
      @Override
      public void onPartialResultSet(PartialResultSet results) {
        addToStream(results);
      }

      @Override
      public void onCompleted() {
        addToStream(END_OF_STREAM);
      }

      @Override
      public void onError(SpannerException e) {
        if (statement != null) {
          if (logger.isLoggable(Level.FINEST)) {
            // Include parameter values if logging level is set to FINEST or higher.
            e =
                SpannerExceptionFactory.newSpannerExceptionPreformatted(
                    e.getErrorCode(),
                    String.format("%s - Statement: '%s'", e.getMessage(), statement.toString()),
                    e);
            logger.log(Level.FINEST, "Error executing statement", e);
          } else {
            e =
                SpannerExceptionFactory.newSpannerExceptionPreformatted(
                    e.getErrorCode(),
                    String.format("%s - Statement: '%s'", e.getMessage(), statement.getSql()),
                    e);
          }
        }
        error = e;
        addToStream(END_OF_STREAM);
      }
    }
  }

  /**
   * Wraps an iterator over partial result sets, supporting resuming RPCs on error. This class keeps
   * track of the most recent resume token seen, and will buffer partial result set chunks that do
   * not have a resume token until one is seen or buffer space is exceeded, which reduces the chance
   * of yielding data to the caller that cannot be resumed.
   */
  @VisibleForTesting
  abstract static class ResumableStreamIterator extends AbstractIterator<PartialResultSet>
      implements CloseableIterator<PartialResultSet> {
    private static final RetrySettings STREAMING_RETRY_SETTINGS =
        SpannerStubSettings.newBuilder().executeStreamingSqlSettings().getRetrySettings();
    private static final Logger logger = Logger.getLogger(ResumableStreamIterator.class.getName());
    private final BackOff backOff = newBackOff();
    private final LinkedList<PartialResultSet> buffer = new LinkedList<>();
    private final int maxBufferSize;
    private final Span span;
    private CloseableIterator<PartialResultSet> stream;
    private ByteString resumeToken;
    private boolean finished;
    /**
     * Indicates whether it is currently safe to retry RPCs. This will be {@code false} if we have
     * reached the maximum buffer size without seeing a restart token; in this case, we will drain
     * the buffer and remain in this state until we see a new restart token.
     */
    private boolean safeToRetry = true;

    protected ResumableStreamIterator(int maxBufferSize, String streamName, Span parent) {
      checkArgument(maxBufferSize >= 0);
      this.maxBufferSize = maxBufferSize;
      this.span = tracer.spanBuilderWithExplicitParent(streamName, parent).startSpan();
    }

    private static ExponentialBackOff newBackOff() {
      return new ExponentialBackOff.Builder()
          .setMultiplier(STREAMING_RETRY_SETTINGS.getRetryDelayMultiplier())
          .setInitialIntervalMillis(
              Math.max(10, (int) STREAMING_RETRY_SETTINGS.getInitialRetryDelay().toMillis()))
          .setMaxIntervalMillis(
              Math.max(1000, (int) STREAMING_RETRY_SETTINGS.getMaxRetryDelay().toMillis()))
          .setMaxElapsedTimeMillis(Integer.MAX_VALUE) // Prevent Backoff.STOP from getting returned.
          .build();
    }

    private static void backoffSleep(Context context, BackOff backoff) throws SpannerException {
      backoffSleep(context, nextBackOffMillis(backoff));
    }

    private static long nextBackOffMillis(BackOff backoff) throws SpannerException {
      try {
        return backoff.nextBackOffMillis();
      } catch (IOException e) {
        throw newSpannerException(ErrorCode.INTERNAL, e.getMessage(), e);
      }
    }

    private static void backoffSleep(Context context, long backoffMillis) throws SpannerException {
      tracer
          .getCurrentSpan()
          .addAnnotation(
              "Backing off",
              ImmutableMap.of("Delay", AttributeValue.longAttributeValue(backoffMillis)));
      final CountDownLatch latch = new CountDownLatch(1);
      final Context.CancellationListener listener =
          new Context.CancellationListener() {
            @Override
            public void cancelled(Context context) {
              // Wakeup on cancellation / DEADLINE_EXCEEDED.
              latch.countDown();
            }
          };

      context.addListener(listener, DirectExecutor.INSTANCE);
      try {
        if (backoffMillis == BackOff.STOP) {
          // Highly unlikely but we handle it just in case.
          backoffMillis = STREAMING_RETRY_SETTINGS.getMaxRetryDelay().toMillis();
        }
        if (latch.await(backoffMillis, TimeUnit.MILLISECONDS)) {
          // Woken by context cancellation.
          throw newSpannerExceptionForCancellation(context, null);
        }
      } catch (InterruptedException interruptExcept) {
        throw newSpannerExceptionForCancellation(context, interruptExcept);
      } finally {
        context.removeListener(listener);
      }
    }

    private enum DirectExecutor implements Executor {
      INSTANCE;

      @Override
      public void execute(Runnable command) {
        command.run();
      }
    }

    abstract CloseableIterator<PartialResultSet> startStream(@Nullable ByteString resumeToken);

    @Override
    public void close(@Nullable String message) {
      if (stream != null) {
        stream.close(message);
        span.end(TraceUtil.END_SPAN_OPTIONS);
        stream = null;
      }
    }

    @Override
    public boolean isWithBeginTransaction() {
      return stream != null && stream.isWithBeginTransaction();
    }

    @Override
    protected PartialResultSet computeNext() {
      Context context = Context.current();
      while (true) {
        // Eagerly start stream before consuming any buffered items.
        if (stream == null) {
          span.addAnnotation(
              "Starting/Resuming stream",
              ImmutableMap.of(
                  "ResumeToken",
                  AttributeValue.stringAttributeValue(
                      resumeToken == null ? "null" : resumeToken.toStringUtf8())));
          try (Scope s = tracer.withSpan(span)) {
            // When start a new stream set the Span as current to make the gRPC Span a child of
            // this Span.
            stream = checkNotNull(startStream(resumeToken));
          }
        }
        // Buffer contains items up to a resume token or has reached capacity: flush.
        if (!buffer.isEmpty()
            && (finished || !safeToRetry || !buffer.getLast().getResumeToken().isEmpty())) {
          return buffer.pop();
        }
        try {
          if (stream.hasNext()) {
            PartialResultSet next = stream.next();
            boolean hasResumeToken = !next.getResumeToken().isEmpty();
            if (hasResumeToken) {
              resumeToken = next.getResumeToken();
              safeToRetry = true;
            }
            // If the buffer is empty and this chunk has a resume token or we cannot resume safely
            // anyway, we can yield it immediately rather than placing it in the buffer to be
            // returned on the next iteration.
            if ((hasResumeToken || !safeToRetry) && buffer.isEmpty()) {
              return next;
            }
            buffer.add(next);
            if (buffer.size() > maxBufferSize && buffer.getLast().getResumeToken().isEmpty()) {
              // We need to flush without a restart token.  Errors encountered until we see
              // such a token will fail the read.
              safeToRetry = false;
            }
          } else {
            finished = true;
            if (buffer.isEmpty()) {
              endOfData();
              return null;
            }
          }
        } catch (SpannerException e) {
          if (safeToRetry && e.isRetryable()) {
            span.addAnnotation(
                "Stream broken. Safe to retry", TraceUtil.getExceptionAnnotations(e));
            logger.log(Level.FINE, "Retryable exception, will sleep and retry", e);
            // Truncate any items in the buffer before the last retry token.
            while (!buffer.isEmpty() && buffer.getLast().getResumeToken().isEmpty()) {
              buffer.removeLast();
            }
            assert buffer.isEmpty() || buffer.getLast().getResumeToken().equals(resumeToken);
            stream = null;
            try (Scope s = tracer.withSpan(span)) {
              long delay = e.getRetryDelayInMillis();
              if (delay != -1) {
                backoffSleep(context, delay);
              } else {
                backoffSleep(context, backOff);
              }
            }

            continue;
          }
          span.addAnnotation("Stream broken. Not safe to retry");
          TraceUtil.setWithFailure(span, e);
          throw e;
        } catch (RuntimeException e) {
          span.addAnnotation("Stream broken. Not safe to retry");
          TraceUtil.setWithFailure(span, e);
          throw e;
        }
      }
    }
  }

  static double valueProtoToFloat64(com.google.protobuf.Value proto) {
    if (proto.getKindCase() == KindCase.STRING_VALUE) {
      switch (proto.getStringValue()) {
        case "-Infinity":
          return Double.NEGATIVE_INFINITY;
        case "Infinity":
          return Double.POSITIVE_INFINITY;
        case "NaN":
          return Double.NaN;
        default:
          // Fall-through to handling below to produce an error.
      }
    }
    if (proto.getKindCase() != KindCase.NUMBER_VALUE) {
      throw newSpannerException(
          ErrorCode.INTERNAL,
          "Invalid value for column type "
              + Type.float64()
              + " expected NUMBER_VALUE or STRING_VALUE with value one of"
              + " \"Infinity\", \"-Infinity\", or \"NaN\" but was "
              + proto.getKindCase()
              + (proto.getKindCase() == KindCase.STRING_VALUE
                  ? " with value \"" + proto.getStringValue() + "\""
                  : ""));
    }
    return proto.getNumberValue();
  }

  static NullPointerException throwNotNull(int columnIndex) {
    throw new NullPointerException(
        "Cannot call array getter for column " + columnIndex + " with null elements");
  }

  protected abstract GrpcStruct currRow();

  @Override
  public Struct getCurrentRowAsStruct() {
    return currRow().immutableCopy();
  }

  @Override
  protected boolean getBooleanInternal(int columnIndex) {
    return currRow().getBooleanInternal(columnIndex);
  }

  @Override
  protected long getLongInternal(int columnIndex) {
    return currRow().getLongInternal(columnIndex);
  }

  @Override
  protected double getDoubleInternal(int columnIndex) {
    return currRow().getDoubleInternal(columnIndex);
  }

  @Override
  protected BigDecimal getBigDecimalInternal(int columnIndex) {
    return currRow().getBigDecimalInternal(columnIndex);
  }

  @Override
  protected String getStringInternal(int columnIndex) {
    return currRow().getStringInternal(columnIndex);
  }

  @Override
  protected ByteArray getBytesInternal(int columnIndex) {
    return currRow().getBytesInternal(columnIndex);
  }

  @Override
  protected Timestamp getTimestampInternal(int columnIndex) {
    return currRow().getTimestampInternal(columnIndex);
  }

  @Override
  protected Date getDateInternal(int columnIndex) {
    return currRow().getDateInternal(columnIndex);
  }

  @Override
  protected Value getValueInternal(int columnIndex) {
    return currRow().getValueInternal(columnIndex);
  }

  @Override
  protected boolean[] getBooleanArrayInternal(int columnIndex) {
    return currRow().getBooleanArrayInternal(columnIndex);
  }

  @Override
  protected List<Boolean> getBooleanListInternal(int columnIndex) {
    return currRow().getBooleanListInternal(columnIndex);
  }

  @Override
  protected long[] getLongArrayInternal(int columnIndex) {
    return currRow().getLongArrayInternal(columnIndex);
  }

  @Override
  protected List<Long> getLongListInternal(int columnIndex) {
    return currRow().getLongListInternal(columnIndex);
  }

  @Override
  protected double[] getDoubleArrayInternal(int columnIndex) {
    return currRow().getDoubleArrayInternal(columnIndex);
  }

  @Override
  protected List<Double> getDoubleListInternal(int columnIndex) {
    return currRow().getDoubleListInternal(columnIndex);
  }

  @Override
  protected List<BigDecimal> getBigDecimalListInternal(int columnIndex) {
    return currRow().getBigDecimalListInternal(columnIndex);
  }

  @Override
  protected List<String> getStringListInternal(int columnIndex) {
    return currRow().getStringListInternal(columnIndex);
  }

  @Override
  protected List<ByteArray> getBytesListInternal(int columnIndex) {
    return currRow().getBytesListInternal(columnIndex);
  }

  @Override
  protected List<Timestamp> getTimestampListInternal(int columnIndex) {
    return currRow().getTimestampListInternal(columnIndex);
  }

  @Override
  protected List<Date> getDateListInternal(int columnIndex) {
    return currRow().getDateListInternal(columnIndex);
  }

  @Override
  protected List<Struct> getStructListInternal(int columnIndex) {
    return currRow().getStructListInternal(columnIndex);
  }

  @Override
  public boolean isNull(int columnIndex) {
    return currRow().isNull(columnIndex);
  }
}
