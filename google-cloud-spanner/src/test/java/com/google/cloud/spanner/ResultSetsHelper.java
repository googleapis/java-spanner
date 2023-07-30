package com.google.cloud.spanner;

import com.google.cloud.spanner.AbstractResultSet.CloseableIterator;
import com.google.cloud.spanner.AbstractResultSet.GrpcResultSet;
import com.google.cloud.spanner.AbstractResultSet.Listener;
import com.google.protobuf.ListValue;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.Transaction;
import java.util.Iterator;
import javax.annotation.Nullable;

public class ResultSetsHelper {

  /**
   * Creates a {@link ResultSets} from a proto {@link com.google.spanner.v1.ResultSet}.
   *
   * <p>Note: The returned result holds a reference to the proto that is passed in to this method.
   * Changing the proto will change the result that is returned.
   */
  public static ResultSet fromProto(com.google.spanner.v1.ResultSet proto) {
    Iterator<ListValue> iterator = proto.getRowsList().iterator();
    return new GrpcResultSet(
        new CloseableIterator<PartialResultSet>() {
          private boolean first = true;

          @Override
          public void close(@Nullable String message) {}

          @Override
          public boolean isWithBeginTransaction() {
            return false;
          }

          @Override
          public boolean hasNext() {
            return first || iterator.hasNext();
          }

          @Override
          public PartialResultSet next() {
            if (!hasNext()) {
              throw new IllegalStateException();
            }
            PartialResultSet.Builder builder = PartialResultSet.newBuilder();
            if (first) {
              builder.setMetadata(proto.getMetadata());
              first = false;
            }
            if (iterator.hasNext()) {
              builder.addAllValues(iterator.next().getValuesList());
            }
            if (!iterator.hasNext()) {
              builder.setStats(proto.getStats());
            }
            return builder.build();
          }
        },
        new Listener() {
          @Override
          public void onTransactionMetadata(Transaction transaction, boolean shouldIncludeId)
              throws SpannerException {}

          @Override
          public SpannerException onError(SpannerException e, boolean withBeginTransaction) {
            return e;
          }

          @Override
          public void onDone(boolean withBeginTransaction) {}
        });
  }
}
