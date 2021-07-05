/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner;

import com.google.api.gax.grpc.testing.MockGrpcService;
import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.spanner.AbstractResultSet.GrpcStruct;
import com.google.cloud.spanner.SessionPool.SessionPoolTransactionContext;
import com.google.cloud.spanner.TransactionRunnerImpl.TransactionContextImpl;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value.KindCase;
import com.google.rpc.Code;
import com.google.rpc.ResourceInfo;
import com.google.rpc.RetryInfo;
import com.google.spanner.v1.BatchCreateSessionsRequest;
import com.google.spanner.v1.BatchCreateSessionsResponse;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.CommitResponse;
import com.google.spanner.v1.CreateSessionRequest;
import com.google.spanner.v1.DeleteSessionRequest;
import com.google.spanner.v1.ExecuteBatchDmlRequest;
import com.google.spanner.v1.ExecuteBatchDmlResponse;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.GetSessionRequest;
import com.google.spanner.v1.ListSessionsRequest;
import com.google.spanner.v1.ListSessionsResponse;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.Partition;
import com.google.spanner.v1.PartitionQueryRequest;
import com.google.spanner.v1.PartitionReadRequest;
import com.google.spanner.v1.PartitionResponse;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.ResultSet;
import com.google.spanner.v1.ResultSetMetadata;
import com.google.spanner.v1.ResultSetStats;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.Session;
import com.google.spanner.v1.SpannerGrpc.SpannerImplBase;
import com.google.spanner.v1.StructType;
import com.google.spanner.v1.StructType.Field;
import com.google.spanner.v1.Transaction;
import com.google.spanner.v1.TransactionOptions;
import com.google.spanner.v1.TransactionOptions.ModeCase;
import com.google.spanner.v1.TransactionOptions.ReadWrite;
import com.google.spanner.v1.TransactionSelector;
import com.google.spanner.v1.Type;
import com.google.spanner.v1.TypeCode;
import io.grpc.Metadata;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.protobuf.lite.ProtoLiteUtils;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.threeten.bp.Instant;

/**
 * In-process mock implementation of a Cloud Spanner server. The user must specify the results the
 * server should return for queries and updates by calling {@link
 * MockSpannerServiceImpl#putStatementResult(StatementResult)}. Note that the SQL statements that
 * are put as statement results are not parsed and are only used as a key know which result to
 * return. This also means that small stylistic differences between two SQL statements will be
 * regarded as two different SQL statements by the mock server.
 *
 * <p>Session and transaction creation is automatically managed and does not need to be mocked.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * Statement statementSelect1 = Statement.of("SELECT 1 AS COL1");
 * com.google.spanner.v1.ResultSetMetadata select1Metadata =
 *     com.google.spanner.v1.ResultSetMetadata.newBuilder()
 *         .setRowType(
 *             StructType.newBuilder()
 *                 .addFields(
 *                     Field.newBuilder()
 *                         .setName("COL1")
 *                         .setType(
 *                             com.google.spanner.v1.Type.newBuilder()
 *                                 .setCode(TypeCode.INT64)
 *                                 .build())
 *                         .build())
 *                 .build())
 *         .build();
 * com.google.spanner.v1.ResultSet select1ResultSet =
 *     com.google.spanner.v1.ResultSet.newBuilder()
 *         .addRows(
 *             ListValue.newBuilder()
 *                 .addValues(com.google.protobuf.Value.newBuilder().setStringValue("1").build())
 *                 .build())
 *         .setMetadata(select1Metadata)
 *         .build();
 * Statement updateStatement = Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2");
 * MockSpannerServiceImpl mockSpanner = new MockSpannerServiceImpl();
 * mockSpanner.putStatementResult(StatementResult.of(statementSelect1, select1ResultSet));
 * mockSpanner.putStatementResult(StatementResult.of(updateStatement, 1L));
 * MockServiceHelper serviceHelper =
 *     new MockServiceHelper("in-process-1", Arrays.<MockGrpcService>asList(mockSpanner));
 * serviceHelper.start();
 *
 * serviceHelper.reset();
 * TransportChannelProvider channelProvider = serviceHelper.createChannelProvider();
 * SpannerSettings settings =
 *     SpannerSettings.newBuilder()
 *         .setTransportChannelProvider(channelProvider)
 *         .setCredentialsProvider(NoCredentialsProvider.create())
 *         .build();
 * SpannerClient spannerClient = SpannerClient.create(settings);
 * Spanner spanner =
 *     SpannerOptions.newBuilder()
 *         .setChannelProvider(channelProvider)
 *         .setCredentials(NoCredentials.getInstance())
 *         .build()
 *         .getService();
 * DatabaseClient dbClient =
 *     spanner.getDatabaseClient(DatabaseId.of("PROJECT_ID", "INSTANCE_ID", "DATABASE_ID"));
 * try (ResultSet resultSet =
 *     dbClient.singleUse().executeQuery(Statement.of("SELECT 1 AS COL1"))) {
 *   while (resultSet.next()) {
 *     System.out.println("COL1: " + resultSet.getLong("COL1"));
 *   }
 * }
 * long updateCount =
 *     dbClient
 *         .readWriteTransaction()
 *         .run(transaction ->
 *             transaction.executeUpdate(Statement.of("UPDATE FOO SET BAR=1 WHERE BAZ=2"))
 *          );
 * System.out.println("Update count: " + updateCount);
 * spannerClient.close();
 * }</pre>
 */
public class MockSpannerServiceImpl extends SpannerImplBase implements MockGrpcService {
  private static class PartialResultSetsIterator implements Iterator<PartialResultSet> {
    private static final int MAX_ROWS_IN_CHUNK = 1;

    private final ResultSet resultSet;
    private boolean hasNext;
    private boolean first = true;
    private int currentRow = 0;

    private PartialResultSetsIterator(ResultSet resultSet) {
      this.resultSet = resultSet;
      this.hasNext = true;
    }

    @Override
    public boolean hasNext() {
      return hasNext;
    }

    @Override
    public PartialResultSet next() {
      PartialResultSet.Builder builder = PartialResultSet.newBuilder();
      if (first) {
        builder.setMetadata(resultSet.getMetadata());
        first = false;
      }
      int recordCount = 0;
      while (recordCount < MAX_ROWS_IN_CHUNK && currentRow < resultSet.getRowsCount()) {
        builder.addAllValues(resultSet.getRows(currentRow).getValuesList());
        builder.setResumeToken(ByteString.copyFromUtf8(String.format("%010d", currentRow)));
        recordCount++;
        currentRow++;
      }
      builder.setResumeToken(ByteString.copyFromUtf8(String.format("%09d", currentRow)));
      hasNext = currentRow < resultSet.getRowsCount();
      return builder.build();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /** The result of a statement that is executed on a {@link MockSpannerServiceImpl}. */
  public static class StatementResult {
    private enum StatementResultType {
      RESULT_SET,
      UPDATE_COUNT,
      EXCEPTION
    }

    private final StatementResultType type;
    private final Statement statement;
    private final Long updateCount;
    private final Deque<ResultSet> resultSets;
    private final StatusRuntimeException exception;

    /** Creates a {@link StatementResult} for a query that returns a {@link ResultSet}. */
    public static StatementResult query(Statement statement, ResultSet resultSet) {
      return new StatementResult(statement, resultSet);
    }

    /**
     * Creates a {@link StatementResult} for a query that returns a {@link ResultSet} the first
     * time, and a different {@link ResultSet} for all subsequent calls.
     */
    public static StatementResult queryAndThen(
        Statement statement, ResultSet resultSet, ResultSet next) {
      return new StatementResult(statement, resultSet);
    }

    /** Creates a {@link StatementResult} for a read request. */
    public static StatementResult read(
        String table, KeySet keySet, Iterable<String> columns, ResultSet resultSet) {
      return new StatementResult(table, keySet, columns, resultSet);
    }

    /** Creates a {@link StatementResult} for a DML statement that returns an update count. */
    public static StatementResult update(Statement statement, long updateCount) {
      return new StatementResult(statement, updateCount);
    }

    /** Creates a {@link StatementResult} for statement that should return an error. */
    public static StatementResult exception(Statement statement, StatusRuntimeException exception) {
      return new StatementResult(statement, exception);
    }

    private static class KeepLastElementDeque<E> extends LinkedList<E> {
      private static <E> KeepLastElementDeque<E> singleton(E item) {
        return new KeepLastElementDeque<>(Collections.singleton(item));
      }

      private static <E> KeepLastElementDeque<E> of(E first, E second) {
        return new KeepLastElementDeque<>(Arrays.asList(first, second));
      }

      private KeepLastElementDeque(Collection<E> coll) {
        super(coll);
      }

      @Override
      public E pop() {
        return this.size() == 1 ? super.peek() : super.pop();
      }
    }

    /**
     * Creates a {@link Statement} for a read statement. This {@link Statement} can be used to mock
     * a result for a read request.
     */
    public static Statement createReadStatement(
        String table, KeySet keySet, Iterable<String> columns) {
      Preconditions.checkNotNull(table);
      Preconditions.checkNotNull(keySet);
      Preconditions.checkNotNull(columns);
      Preconditions.checkArgument(
          isValidKeySet(keySet),
          "Currently only KeySet.all() and KeySet.singleKey(Key.of()) are supported for read statements");
      StringBuilder builder = new StringBuilder("SELECT ");
      boolean first = true;
      for (String col : columns) {
        if (!first) {
          builder.append(", ");
        }
        builder.append(col);
        first = false;
      }
      builder.append(" FROM ").append(table);
      if (keySet.isAll()) {
        builder.append(" WHERE 1=1");
      } else {
        builder.append(" WHERE ID=1");
      }
      return Statement.of(builder.toString());
    }

    private static boolean isValidKeySet(KeySet keySet) {
      if (keySet.isAll()) {
        return true;
      }
      int keys = 0;
      for (Key key : keySet.getKeys()) {
        keys++;
        if (key.size() != 0) {
          return false;
        }
      }
      return keys == 1;
    }

    private StatementResult(Statement statement, Long updateCount) {
      this.statement = Preconditions.checkNotNull(statement);
      this.updateCount = Preconditions.checkNotNull(updateCount);
      this.resultSets = null;
      this.exception = null;
      this.type = StatementResultType.UPDATE_COUNT;
    }

    private StatementResult(Statement statement, ResultSet resultSet) {
      this.statement = Preconditions.checkNotNull(statement);
      this.resultSets = KeepLastElementDeque.singleton(Preconditions.checkNotNull(resultSet));
      this.updateCount = null;
      this.exception = null;
      this.type = StatementResultType.RESULT_SET;
    }

    private StatementResult(Statement statement, ResultSet resultSet, ResultSet andThen) {
      this.statement = Preconditions.checkNotNull(statement);
      this.resultSets =
          KeepLastElementDeque.of(
              Preconditions.checkNotNull(resultSet), Preconditions.checkNotNull(andThen));
      this.updateCount = null;
      this.exception = null;
      this.type = StatementResultType.RESULT_SET;
    }

    private StatementResult(
        String table, KeySet keySet, Iterable<String> columns, ResultSet resultSet) {
      this.statement = createReadStatement(table, keySet, columns);
      this.resultSets = KeepLastElementDeque.singleton(Preconditions.checkNotNull(resultSet));
      this.updateCount = null;
      this.exception = null;
      this.type = StatementResultType.RESULT_SET;
    }

    private StatementResult(Statement statement, StatusRuntimeException exception) {
      this.statement = Preconditions.checkNotNull(statement);
      this.exception = Preconditions.checkNotNull(exception);
      this.resultSets = null;
      this.updateCount = null;
      this.type = StatementResultType.EXCEPTION;
    }

    private StatementResultType getType() {
      return type;
    }

    private ResultSet getResultSet() {
      Preconditions.checkState(
          type == StatementResultType.RESULT_SET,
          "This statement result does not contain a result set");
      return resultSets.pop();
    }

    private Long getUpdateCount() {
      Preconditions.checkState(
          type == StatementResultType.UPDATE_COUNT,
          "This statement result does not contain an update count");
      return updateCount;
    }

    private StatusRuntimeException getException() {
      Preconditions.checkState(
          type == StatementResultType.EXCEPTION,
          "This statement result does not contain an exception");
      return exception;
    }
  }

  /** Class for simulating execution time of server calls. */
  public static class SimulatedExecutionTime {
    private static final Random RANDOM = new Random();
    private final int minimumExecutionTime;
    private final int randomExecutionTime;
    private final Queue<Exception> exceptions;
    private final boolean stickyException;
    private final Queue<Long> streamIndices;

    /**
     * Creates a simulated execution time that will always be somewhere between <code>
     * minimumExecutionTime+randomExecutionTime</code> milliseconds long.
     *
     * @param minimumExecutionTime The minimum number of milliseconds the execution of the method
     *     should be.
     * @param randomExecutionTime The maximum random number of milliseconds that should be added to
     *     the minimum execution time.
     * @return a {@link SimulatedExecutionTime} that can be set as the execution time of a server
     *     call on a {@link MockSpannerServiceImpl}.
     */
    public static SimulatedExecutionTime ofMinimumAndRandomTime(
        int minimumExecutionTime, int randomExecutionTime) {
      return new SimulatedExecutionTime(minimumExecutionTime, randomExecutionTime);
    }

    public static SimulatedExecutionTime none() {
      return new SimulatedExecutionTime(0, 0);
    }

    public static SimulatedExecutionTime ofException(Exception exception) {
      return new SimulatedExecutionTime(
          0, 0, Collections.singletonList(exception), false, Collections.emptySet());
    }

    public static SimulatedExecutionTime ofStickyException(Exception exception) {
      return new SimulatedExecutionTime(
          0, 0, Collections.singletonList(exception), true, Collections.emptySet());
    }

    public static SimulatedExecutionTime ofStreamException(Exception exception, long streamIndex) {
      return new SimulatedExecutionTime(
          0, 0, Collections.singletonList(exception), false, Collections.singleton(streamIndex));
    }

    public static SimulatedExecutionTime stickyDatabaseNotFoundException(String name) {
      return ofStickyException(
          SpannerExceptionFactoryTest.newStatusDatabaseNotFoundException(name));
    }

    public static SimulatedExecutionTime ofExceptions(Collection<? extends Exception> exceptions) {
      return new SimulatedExecutionTime(0, 0, exceptions, false, Collections.emptySet());
    }

    public static SimulatedExecutionTime ofMinimumAndRandomTimeAndExceptions(
        int minimumExecutionTime,
        int randomExecutionTime,
        Collection<? extends Exception> exceptions) {
      return new SimulatedExecutionTime(
          minimumExecutionTime, randomExecutionTime, exceptions, false, Collections.emptySet());
    }

    private SimulatedExecutionTime(int minimum, int random) {
      this(minimum, random, Collections.emptyList(), false, Collections.emptySet());
    }

    private SimulatedExecutionTime(
        int minimum,
        int random,
        Collection<? extends Exception> exceptions,
        boolean stickyException,
        Collection<Long> streamIndices) {
      Preconditions.checkArgument(minimum >= 0, "Minimum execution time must be >= 0");
      Preconditions.checkArgument(random >= 0, "Random execution time must be >= 0");
      this.minimumExecutionTime = minimum;
      this.randomExecutionTime = random;
      this.exceptions = new LinkedList<>(exceptions);
      this.stickyException = stickyException;
      this.streamIndices = new LinkedList<>(streamIndices);
    }

    void simulateExecutionTime(
        Queue<Exception> globalExceptions,
        boolean stickyGlobalExceptions,
        CountDownLatch freezeLock) {
      Uninterruptibles.awaitUninterruptibly(freezeLock);
      checkException(globalExceptions, stickyGlobalExceptions);
      if (streamIndices.isEmpty()) {
        checkException(this.exceptions, stickyException);
      }
      if (minimumExecutionTime > 0 || randomExecutionTime > 0) {
        Uninterruptibles.sleepUninterruptibly(
            (randomExecutionTime == 0 ? 0 : RANDOM.nextInt(randomExecutionTime))
                + minimumExecutionTime,
            TimeUnit.MILLISECONDS);
      }
    }

    private static void checkException(Queue<Exception> exceptions, boolean keepException) {
      Exception e = keepException ? exceptions.peek() : exceptions.poll();
      if (e != null) {
        Throwables.throwIfUnchecked(e);
        throw Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException();
      }
    }

    private static void checkStreamException(
        long streamIndex, Queue<Exception> exceptions, Queue<Long> streamIndices) {
      Exception e = exceptions.peek();
      Long index = streamIndices.peek();
      if (e != null && index != null && index == streamIndex) {
        exceptions.poll();
        streamIndices.poll();
        Throwables.throwIfUnchecked(e);
        throw Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException();
      }
    }
  }

  public static final SimulatedExecutionTime NO_EXECUTION_TIME = SimulatedExecutionTime.none();

  private final Random random = new Random();
  private double abortProbability = 0.0010D;

  private final Object lock = new Object();
  private Deque<AbstractMessage> requests = new ConcurrentLinkedDeque<>();
  private volatile CountDownLatch freezeLock = new CountDownLatch(0);
  private Queue<Exception> exceptions = new ConcurrentLinkedQueue<>();
  private boolean stickyGlobalExceptions = false;
  private ConcurrentMap<Statement, StatementResult> statementResults = new ConcurrentHashMap<>();
  private ConcurrentMap<Statement, Long> statementGetCounts = new ConcurrentHashMap<>();
  private ConcurrentMap<String, StatementResult> partialStatementResults =
      new ConcurrentHashMap<>();
  private ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
  private ConcurrentMap<String, Instant> sessionLastUsed = new ConcurrentHashMap<>();
  private ConcurrentMap<ByteString, Transaction> transactions = new ConcurrentHashMap<>();
  private final Queue<ByteString> transactionsStarted = new ConcurrentLinkedQueue<>();
  private ConcurrentMap<ByteString, Boolean> isPartitionedDmlTransaction =
      new ConcurrentHashMap<>();
  private ConcurrentMap<ByteString, Boolean> abortedTransactions = new ConcurrentHashMap<>();
  private final AtomicBoolean abortNextTransaction = new AtomicBoolean();
  private final AtomicBoolean abortNextStatement = new AtomicBoolean();
  private final AtomicBoolean ignoreNextInlineBeginRequest = new AtomicBoolean();
  private ConcurrentMap<String, AtomicLong> transactionCounters = new ConcurrentHashMap<>();
  private ConcurrentMap<String, List<ByteString>> partitionTokens = new ConcurrentHashMap<>();
  private ConcurrentMap<ByteString, Instant> transactionLastUsed = new ConcurrentHashMap<>();
  private int maxNumSessionsInOneBatch = 100;
  private int maxTotalSessions = Integer.MAX_VALUE;
  private AtomicInteger numSessionsCreated = new AtomicInteger();

  private SimulatedExecutionTime beginTransactionExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime commitExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime batchCreateSessionsExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime createSessionExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime deleteSessionExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime executeBatchDmlExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime executeSqlExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime executeStreamingSqlExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime getSessionExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime listSessionsExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime partitionQueryExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime partitionReadExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime readExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime rollbackExecutionTime = NO_EXECUTION_TIME;
  private SimulatedExecutionTime streamingReadExecutionTime = NO_EXECUTION_TIME;

  private String generateSessionName(String database) {
    return String.format("%s/sessions/%s", database, UUID.randomUUID().toString());
  }

  private ByteString generateTransactionName(String session) {
    AtomicLong counter = transactionCounters.get(session);
    if (counter == null) {
      counter = new AtomicLong();
      transactionCounters.put(session, counter);
    }
    return ByteString.copyFromUtf8(
        String.format("%s/transactions/%d", session, counter.incrementAndGet()));
  }

  private ByteString generatePartitionToken(String session, ByteString transactionId) {
    ByteString token = ByteString.copyFromUtf8(UUID.randomUUID().toString());
    String key = partitionKey(session, transactionId);
    List<ByteString> tokens = partitionTokens.computeIfAbsent(key, k -> new ArrayList<>(5));
    tokens.add(token);
    return token;
  }

  private String partitionKey(String session, ByteString transactionId) {
    return String.format("%s/transactions/%s", session, transactionId.toStringUtf8());
  }

  private Timestamp getCurrentGoogleTimestamp() {
    long current = System.currentTimeMillis();
    long seconds = TimeUnit.MILLISECONDS.toSeconds(current);
    int nanos = (int) TimeUnit.MILLISECONDS.toNanos(current - TimeUnit.SECONDS.toMillis(seconds));
    return Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos).build();
  }

  /**
   * Puts a result that will be returned by this service. {@link StatementResult#statement} will be
   * used as a key for the result, and any existing {@link StatementResult} for the same {@link
   * Statement} will be overwritten.
   */
  public void putStatementResult(StatementResult result) {
    Preconditions.checkNotNull(result);
    synchronized (lock) {
      statementResults.put(result.statement, result);
    }
  }

  public void putStatementResults(StatementResult... results) {
    synchronized (lock) {
      for (StatementResult result : results) {
        statementResults.put(result.statement, result);
      }
    }
  }

  public void putPartialStatementResult(StatementResult result) {
    synchronized (lock) {
      partialStatementResults.put(result.statement.getSql(), result);
    }
  }

  private StatementResult getResult(Statement statement) {
    StatementResult res;
    synchronized (lock) {
      res = statementResults.get(statement);
      if (statementGetCounts.containsKey(statement)) {
        statementGetCounts.put(statement, statementGetCounts.get(statement) + 1L);
      } else {
        statementGetCounts.put(statement, 1L);
      }
      if (res == null) {
        for (String partialSql : partialStatementResults.keySet()) {
          if (statement.getSql().startsWith(partialSql)) {
            res = partialStatementResults.get(partialSql);
          }
        }
      }
    }
    if (res == null) {
      throw Status.INTERNAL
          .withDescription(
              String.format(
                  "There is no result registered for the statement: %s\n"
                      + "Call TestSpannerImpl#addStatementResult(StatementResult) before executing the statement.",
                  statement.toString()))
          .asRuntimeException();
    }
    return res;
  }

  /** Sets the probability that this mock server aborts a read/write transaction at random. */
  public void setAbortProbability(double probability) {
    Preconditions.checkArgument(
        probability >= 0D && probability <= 1D, "Probability must be >= 0 and <= 1");
    this.abortProbability = probability;
  }

  /**
   * Instruct the mock server to abort the specified transaction. Use this method to test handling
   * of {@link AbortedException} in your code.
   */
  public void abortTransaction(TransactionContext transactionContext) {
    Preconditions.checkNotNull(transactionContext);
    if (transactionContext instanceof SessionPoolTransactionContext) {
      transactionContext = ((SessionPoolTransactionContext) transactionContext).delegate;
    }
    if (transactionContext instanceof TransactionContextImpl) {
      TransactionContextImpl impl = (TransactionContextImpl) transactionContext;
      ByteString id =
          impl.getTransactionSelector() == null ? null : impl.getTransactionSelector().getId();
      if (id != null) {
        markAbortedTransaction(id);
      }
    } else {
      throw new IllegalArgumentException(
          "Unsupported TransactionContext type: " + transactionContext.getClass().getName());
    }
  }

  /** Instruct the mock server to abort the next transaction that is created. */
  public void abortNextTransaction() {
    abortNextTransaction.set(true);
  }

  /** Instructs the mock server to abort the transaction of the next statement that is executed. */
  public void abortNextStatement() {
    abortNextStatement.set(true);
  }

  /** Instruct the mock server to abort all transactions currently active on the server. */
  public void abortAllTransactions() {
    for (ByteString id : transactions.keySet()) {
      markAbortedTransaction(id);
    }
  }

  public void ignoreNextInlineBeginRequest() {
    ignoreNextInlineBeginRequest.set(true);
  }

  public void freeze() {
    freezeLock = new CountDownLatch(1);
  }

  public void unfreeze() {
    freezeLock.countDown();
  }

  public void setMaxSessionsInOneBatch(int max) {
    this.maxNumSessionsInOneBatch = max;
  }

  public void setMaxTotalSessions(int max) {
    this.maxTotalSessions = max;
  }

  @Override
  public void batchCreateSessions(
      BatchCreateSessionsRequest request,
      StreamObserver<BatchCreateSessionsResponse> responseObserver) {
    requests.add(request);
    Preconditions.checkNotNull(request.getDatabase());
    String name = null;
    try {
      if (request.getSessionCount() <= 0) {
        throw Status.INVALID_ARGUMENT
            .withDescription("Session count must be >= 0")
            .asRuntimeException();
      }
      batchCreateSessionsExecutionTime.simulateExecutionTime(
          exceptions, stickyGlobalExceptions, freezeLock);
      if (sessions.size() >= maxTotalSessions) {
        throw Status.RESOURCE_EXHAUSTED
            .withDescription("Maximum number of sessions reached")
            .asRuntimeException();
      }
      Timestamp now = getCurrentGoogleTimestamp();
      BatchCreateSessionsResponse.Builder response = BatchCreateSessionsResponse.newBuilder();
      int maxSessionsToCreate = Math.min(maxNumSessionsInOneBatch, request.getSessionCount());
      for (int i = 0; i < Math.min(maxTotalSessions - sessions.size(), maxSessionsToCreate); i++) {
        name = generateSessionName(request.getDatabase());
        Session session =
            Session.newBuilder()
                .setCreateTime(now)
                .setName(name)
                .setApproximateLastUseTime(now)
                .build();
        Session prev = sessions.putIfAbsent(name, session);
        if (prev == null) {
          if (sessions.size() <= maxTotalSessions) {
            sessionLastUsed.put(name, Instant.now());
            response.addSession(session);
            numSessionsCreated.incrementAndGet();
          } else {
            sessions.remove(name);
          }
        } else {
          // Someone else tried to create a session with the same id. This should not be possible
          throw Status.ALREADY_EXISTS.asRuntimeException();
        }
      }
      responseObserver.onNext(response.build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      if (name != null) {
        sessions.remove(name);
      }
      responseObserver.onError(e);
    } catch (Throwable e) {
      if (name != null) {
        sessions.remove(name);
      }
      responseObserver.onError(
          Status.INTERNAL
              .withDescription("Batch create sessions failed: " + e.getMessage())
              .asRuntimeException());
    }
  }

  @Override
  public void createSession(
      CreateSessionRequest request, StreamObserver<Session> responseObserver) {
    requests.add(request);
    Preconditions.checkNotNull(request.getDatabase());
    String name = generateSessionName(request.getDatabase());
    try {
      createSessionExecutionTime.simulateExecutionTime(
          exceptions, stickyGlobalExceptions, freezeLock);
      Timestamp now = getCurrentGoogleTimestamp();
      Session session =
          Session.newBuilder()
              .setCreateTime(now)
              .setName(name)
              .setApproximateLastUseTime(now)
              .build();
      Session prev = sessions.putIfAbsent(name, session);
      if (prev == null) {
        sessionLastUsed.put(name, Instant.now());
        numSessionsCreated.incrementAndGet();
        responseObserver.onNext(session);
        responseObserver.onCompleted();
      } else {
        // Someone else tried to create a session with the same id. This should not be possible
        responseObserver.onError(Status.ALREADY_EXISTS.asRuntimeException());
      }
    } catch (StatusRuntimeException e) {
      sessions.remove(name);
      responseObserver.onError(e);
    } catch (Throwable e) {
      sessions.remove(name);
      responseObserver.onError(
          Status.INTERNAL
              .withDescription("Create session failed: " + e.getMessage())
              .asRuntimeException());
    }
  }

  @Override
  public void getSession(GetSessionRequest request, StreamObserver<Session> responseObserver) {
    requests.add(request);
    Preconditions.checkNotNull(request.getName());
    try {
      getSessionExecutionTime.simulateExecutionTime(exceptions, stickyGlobalExceptions, freezeLock);
      Session session = sessions.get(request.getName());
      if (session == null) {
        setSessionNotFound(request.getName(), responseObserver);
      } else {
        session =
            session.toBuilder().setApproximateLastUseTime(getCurrentGoogleTimestamp()).build();
        responseObserver.onNext(session);
        responseObserver.onCompleted();
      }
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
    }
  }

  private <T> void setSessionNotFound(String name, StreamObserver<T> responseObserver) {
    ResourceInfo resourceInfo =
        ResourceInfo.newBuilder()
            .setResourceType(SpannerExceptionFactory.SESSION_RESOURCE_TYPE)
            .setResourceName(name)
            .build();
    Metadata.Key<ResourceInfo> key =
        Metadata.Key.of(
            resourceInfo.getDescriptorForType().getFullName() + Metadata.BINARY_HEADER_SUFFIX,
            ProtoLiteUtils.metadataMarshaller(resourceInfo));
    Metadata trailers = new Metadata();
    trailers.put(key, resourceInfo);
    responseObserver.onError(
        Status.NOT_FOUND
            .withDescription(String.format("Session not found: Session with id %s not found", name))
            .asRuntimeException(trailers));
  }

  @Override
  public void listSessions(
      ListSessionsRequest request, StreamObserver<ListSessionsResponse> responseObserver) {
    requests.add(request);
    try {
      listSessionsExecutionTime.simulateExecutionTime(
          exceptions, stickyGlobalExceptions, freezeLock);
      List<Session> res = new ArrayList<>();
      for (Session session : sessions.values()) {
        if (session.getName().startsWith(request.getDatabase())) {
          res.add(
              session.toBuilder().setApproximateLastUseTime(getCurrentGoogleTimestamp()).build());
        }
      }
      res.sort(Comparator.comparing(Session::getName));
      responseObserver.onNext(ListSessionsResponse.newBuilder().addAllSessions(res).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
    }
  }

  @Override
  public void deleteSession(DeleteSessionRequest request, StreamObserver<Empty> responseObserver) {
    requests.add(request);
    Preconditions.checkNotNull(request.getName());
    try {
      deleteSessionExecutionTime.simulateExecutionTime(
          exceptions, stickyGlobalExceptions, freezeLock);
      Session session = sessions.get(request.getName());
      if (session != null) {
        try {
          doDeleteSession(session);
        } catch (Throwable e) {
          responseObserver.onError(Status.INTERNAL.asRuntimeException());
          return;
        }
      }
      responseObserver.onNext(Empty.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    }
  }

  void doDeleteSession(Session session) {
    sessions.remove(session.getName());
    transactionCounters.remove(session.getName());
    sessionLastUsed.remove(session.getName());
  }

  @Override
  public void executeSql(ExecuteSqlRequest request, StreamObserver<ResultSet> responseObserver) {
    requests.add(request);
    Preconditions.checkNotNull(request.getSession());
    Session session = sessions.get(request.getSession());
    if (session == null) {
      setSessionNotFound(request.getSession(), responseObserver);
      return;
    }
    sessionLastUsed.put(session.getName(), Instant.now());
    try {
      executeSqlExecutionTime.simulateExecutionTime(exceptions, stickyGlobalExceptions, freezeLock);
      ByteString transactionId = getTransactionId(session, request.getTransaction());
      simulateAbort(session, transactionId);
      Statement statement =
          buildStatement(request.getSql(), request.getParamTypesMap(), request.getParams());
      StatementResult result = getResult(statement);
      switch (result.getType()) {
        case EXCEPTION:
          throw result.getException();
        case RESULT_SET:
          returnResultSet(
              result.getResultSet(), transactionId, request.getTransaction(), responseObserver);
          break;
        case UPDATE_COUNT:
          if (isPartitionedDmlTransaction(transactionId)) {
            commitTransaction(transactionId);
            responseObserver.onNext(
                ResultSet.newBuilder()
                    .setStats(
                        ResultSetStats.newBuilder()
                            .setRowCountLowerBound(result.getUpdateCount())
                            .build())
                    .build());
          } else {
            responseObserver.onNext(
                ResultSet.newBuilder()
                    .setStats(
                        ResultSetStats.newBuilder()
                            .setRowCountExact(result.getUpdateCount())
                            .build())
                    .setMetadata(
                        ResultSetMetadata.newBuilder()
                            .setTransaction(
                                ignoreNextInlineBeginRequest.getAndSet(false)
                                    ? Transaction.getDefaultInstance()
                                    : Transaction.newBuilder().setId(transactionId).build())
                            .build())
                    .build());
          }
          break;
        default:
          throw new IllegalStateException("Unknown result type: " + result.getType());
      }
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
    }
  }

  private void returnResultSet(
      ResultSet resultSet,
      ByteString transactionId,
      TransactionSelector transactionSelector,
      StreamObserver<ResultSet> responseObserver) {
    ResultSetMetadata metadata = resultSet.getMetadata();
    if (transactionId != null) {
      metadata =
          metadata
              .toBuilder()
              .setTransaction(
                  ignoreNextInlineBeginRequest.getAndSet(false)
                      ? Transaction.getDefaultInstance()
                      : Transaction.newBuilder().setId(transactionId).build())
              .build();
    } else if (transactionSelector.hasBegin() || transactionSelector.hasSingleUse()) {
      Transaction transaction = getTemporaryTransactionOrNull(transactionSelector);
      metadata = metadata.toBuilder().setTransaction(transaction).build();
    }
    resultSet = resultSet.toBuilder().setMetadata(metadata).build();
    responseObserver.onNext(resultSet);
  }

  @Override
  public void executeBatchDml(
      ExecuteBatchDmlRequest request, StreamObserver<ExecuteBatchDmlResponse> responseObserver) {
    requests.add(request);
    Preconditions.checkNotNull(request.getSession());
    Session session = sessions.get(request.getSession());
    if (session == null) {
      setSessionNotFound(request.getSession(), responseObserver);
      return;
    }
    sessionLastUsed.put(session.getName(), Instant.now());
    try {
      executeBatchDmlExecutionTime.simulateExecutionTime(
          exceptions, stickyGlobalExceptions, freezeLock);
      // Get or start transaction
      ByteString transactionId = getTransactionId(session, request.getTransaction());
      if (isPartitionedDmlTransaction(transactionId)) {
        throw Status.FAILED_PRECONDITION
            .withDescription(
                "This transaction is a partitioned DML transaction and cannot be used for batch DML updates.")
            .asRuntimeException();
      }
      simulateAbort(session, transactionId);
      List<StatementResult> results = new ArrayList<>();
      com.google.rpc.Status status =
          com.google.rpc.Status.newBuilder().setCode(Code.OK_VALUE).build();
      resultLoop:
      for (com.google.spanner.v1.ExecuteBatchDmlRequest.Statement statement :
          request.getStatementsList()) {
        try {
          Statement spannerStatement =
              buildStatement(
                  statement.getSql(), statement.getParamTypesMap(), statement.getParams());
          StatementResult res = getResult(spannerStatement);
          switch (res.getType()) {
            case EXCEPTION:
              status =
                  com.google.rpc.Status.newBuilder()
                      .setCode(res.getException().getStatus().getCode().value())
                      .setMessage(res.getException().getMessage())
                      .build();
              break resultLoop;
            case RESULT_SET:
              throw Status.INVALID_ARGUMENT
                  .withDescription("Not a DML statement: " + statement.getSql())
                  .asRuntimeException();
            case UPDATE_COUNT:
              results.add(res);
              break;
            default:
              throw new IllegalStateException("Unknown result type: " + res.getType());
          }
        } catch (StatusRuntimeException e) {
          status =
              com.google.rpc.Status.newBuilder()
                  .setCode(e.getStatus().getCode().value())
                  .setMessage(e.getMessage())
                  .build();
          break;
        } catch (Exception e) {
          status =
              com.google.rpc.Status.newBuilder()
                  .setCode(Code.UNKNOWN_VALUE)
                  .setMessage(e.getMessage())
                  .build();
          break;
        }
      }
      ExecuteBatchDmlResponse.Builder builder = ExecuteBatchDmlResponse.newBuilder();
      for (StatementResult res : results) {
        builder.addResultSets(
            ResultSet.newBuilder()
                .setStats(
                    ResultSetStats.newBuilder().setRowCountExact(res.getUpdateCount()).build())
                .setMetadata(
                    ResultSetMetadata.newBuilder()
                        .setTransaction(
                            ignoreNextInlineBeginRequest.getAndSet(false)
                                ? Transaction.getDefaultInstance()
                                : Transaction.newBuilder().setId(transactionId).build())
                        .build())
                .build());
      }
      builder.setStatus(status);
      responseObserver.onNext(builder.build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
    }
  }

  @Override
  public void executeStreamingSql(
      ExecuteSqlRequest request, StreamObserver<PartialResultSet> responseObserver) {
    requests.add(request);
    Preconditions.checkNotNull(request.getSession());
    Session session = sessions.get(request.getSession());
    if (session == null) {
      setSessionNotFound(request.getSession(), responseObserver);
      return;
    }
    sessionLastUsed.put(session.getName(), Instant.now());
    try {
      Statement statement =
          buildStatement(request.getSql(), request.getParamTypesMap(), request.getParams());
      ByteString transactionId = getTransactionId(session, request.getTransaction());
      boolean isPartitioned = isPartitionedDmlTransaction(transactionId);
      if (isPartitioned) {
        StatementResult firstRes = getResult(statement);
        switch (firstRes.getType()) {
          case EXCEPTION:
            throw firstRes.getException();
          case UPDATE_COUNT:
            returnPartialResultSet(
                session, 0L, !isPartitioned, responseObserver, request.getTransaction(), false);
            break;
          case RESULT_SET:
          default:
            break;
        }
      }
      executeStreamingSqlExecutionTime.simulateExecutionTime(
          exceptions, stickyGlobalExceptions, freezeLock);
      // Get or start transaction
      if (!request.getPartitionToken().isEmpty()) {
        List<ByteString> tokens =
            partitionTokens.get(partitionKey(session.getName(), transactionId));
        if (tokens == null || !tokens.contains(request.getPartitionToken())) {
          throw Status.INVALID_ARGUMENT
              .withDescription(
                  String.format(
                      "Partition token %s is not a valid token for this transaction",
                      request.getPartitionToken()))
              .asRuntimeException();
        }
      }
      simulateAbort(session, transactionId);
      StatementResult res = getResult(statement);
      switch (res.getType()) {
        case EXCEPTION:
          throw res.getException();
        case RESULT_SET:
          returnPartialResultSet(
              res.getResultSet(),
              transactionId,
              request.getTransaction(),
              responseObserver,
              getExecuteStreamingSqlExecutionTime());
          break;
        case UPDATE_COUNT:
          if (isPartitioned) {
            commitTransaction(transactionId);
          }
          returnPartialResultSet(
              session,
              res.getUpdateCount(),
              !isPartitioned,
              responseObserver,
              request.getTransaction());
          break;
        default:
          throw new IllegalStateException("Unknown result type: " + res.getType());
      }
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.withCause(t).asRuntimeException());
    }
  }

  @SuppressWarnings("unchecked")
  private Statement buildStatement(
      String sql, Map<String, Type> paramTypes, com.google.protobuf.Struct params) {
    Statement.Builder builder = Statement.newBuilder(sql);
    for (Entry<String, Type> entry : paramTypes.entrySet()) {
      com.google.protobuf.Value value = params.getFieldsOrThrow(entry.getKey());
      if (value.getKindCase() == KindCase.NULL_VALUE) {
        switch (entry.getValue().getCode()) {
          case ARRAY:
            switch (entry.getValue().getArrayElementType().getCode()) {
              case BOOL:
                builder.bind(entry.getKey()).toBoolArray((Iterable<Boolean>) null);
                break;
              case BYTES:
                builder.bind(entry.getKey()).toBytesArray(null);
                break;
              case DATE:
                builder.bind(entry.getKey()).toDateArray(null);
                break;
              case FLOAT64:
                builder.bind(entry.getKey()).toFloat64Array((Iterable<Double>) null);
                break;
              case INT64:
                builder.bind(entry.getKey()).toInt64Array((Iterable<Long>) null);
                break;
              case STRING:
                builder.bind(entry.getKey()).toStringArray(null);
                break;
              case TIMESTAMP:
                builder.bind(entry.getKey()).toTimestampArray(null);
                break;
              case STRUCT:
              case TYPE_CODE_UNSPECIFIED:
              case UNRECOGNIZED:
              default:
                throw new IllegalArgumentException(
                    "Unknown or invalid array parameter type: "
                        + entry.getValue().getArrayElementType().getCode());
            }
            break;
          case BOOL:
            builder.bind(entry.getKey()).to((Boolean) null);
            break;
          case BYTES:
            builder.bind(entry.getKey()).to((ByteArray) null);
            break;
          case DATE:
            builder.bind(entry.getKey()).to((Date) null);
            break;
          case FLOAT64:
            builder.bind(entry.getKey()).to((Double) null);
            break;
          case INT64:
            builder.bind(entry.getKey()).to((Long) null);
            break;
          case STRING:
            builder.bind(entry.getKey()).to((String) null);
            break;
          case STRUCT:
            builder.bind(entry.getKey()).to((Struct) null);
            break;
          case TIMESTAMP:
            builder.bind(entry.getKey()).to((com.google.cloud.Timestamp) null);
            break;
          case TYPE_CODE_UNSPECIFIED:
          case UNRECOGNIZED:
          default:
            throw new IllegalArgumentException(
                "Unknown parameter type: " + entry.getValue().getCode());
        }
      } else {
        switch (entry.getValue().getCode()) {
          case ARRAY:
            switch (entry.getValue().getArrayElementType().getCode()) {
              case BOOL:
                builder
                    .bind(entry.getKey())
                    .toBoolArray(
                        (Iterable<Boolean>)
                            GrpcStruct.decodeArrayValue(
                                com.google.cloud.spanner.Type.bool(), value.getListValue()));
                break;
              case BYTES:
                builder
                    .bind(entry.getKey())
                    .toBytesArray(
                        (Iterable<ByteArray>)
                            GrpcStruct.decodeArrayValue(
                                com.google.cloud.spanner.Type.bytes(), value.getListValue()));
                break;
              case DATE:
                builder
                    .bind(entry.getKey())
                    .toDateArray(
                        (Iterable<Date>)
                            GrpcStruct.decodeArrayValue(
                                com.google.cloud.spanner.Type.date(), value.getListValue()));
                break;
              case FLOAT64:
                builder
                    .bind(entry.getKey())
                    .toFloat64Array(
                        (Iterable<Double>)
                            GrpcStruct.decodeArrayValue(
                                com.google.cloud.spanner.Type.float64(), value.getListValue()));
                break;
              case INT64:
                builder
                    .bind(entry.getKey())
                    .toInt64Array(
                        (Iterable<Long>)
                            GrpcStruct.decodeArrayValue(
                                com.google.cloud.spanner.Type.int64(), value.getListValue()));
                break;
              case STRING:
                builder
                    .bind(entry.getKey())
                    .toStringArray(
                        (Iterable<String>)
                            GrpcStruct.decodeArrayValue(
                                com.google.cloud.spanner.Type.string(), value.getListValue()));
                break;
              case TIMESTAMP:
                builder
                    .bind(entry.getKey())
                    .toTimestampArray(
                        (Iterable<com.google.cloud.Timestamp>)
                            GrpcStruct.decodeArrayValue(
                                com.google.cloud.spanner.Type.timestamp(), value.getListValue()));
                break;
              case STRUCT:
              case TYPE_CODE_UNSPECIFIED:
              case UNRECOGNIZED:
              default:
                throw new IllegalArgumentException(
                    "Unknown or invalid array parameter type: "
                        + entry.getValue().getArrayElementType().getCode());
            }
            break;
          case BOOL:
            builder.bind(entry.getKey()).to(value.getBoolValue());
            break;
          case BYTES:
            builder.bind(entry.getKey()).to(ByteArray.fromBase64(value.getStringValue()));
            break;
          case DATE:
            builder.bind(entry.getKey()).to(Date.parseDate(value.getStringValue()));
            break;
          case FLOAT64:
            builder.bind(entry.getKey()).to(value.getNumberValue());
            break;
          case INT64:
            builder.bind(entry.getKey()).to(Long.valueOf(value.getStringValue()));
            break;
          case STRING:
            builder.bind(entry.getKey()).to(value.getStringValue());
            break;
          case STRUCT:
            throw new IllegalArgumentException("Struct parameters not (yet) supported");
          case TIMESTAMP:
            builder
                .bind(entry.getKey())
                .to(com.google.cloud.Timestamp.parseTimestamp(value.getStringValue()));
            break;
          case TYPE_CODE_UNSPECIFIED:
          case UNRECOGNIZED:
          default:
            throw new IllegalArgumentException(
                "Unknown parameter type: " + entry.getValue().getCode());
        }
      }
    }
    return builder.build();
  }

  private <T> void setTransactionNotFound(
      ByteString transactionId, StreamObserver<T> responseObserver) {
    responseObserver.onError(
        Status.ABORTED
            .withDescription(
                String.format(
                    "Transaction with id %s not found and has probably been aborted",
                    transactionId.toStringUtf8()))
            .asRuntimeException());
  }

  private <T> void throwTransactionNotFound(ByteString transactionId) {
    Metadata.Key<RetryInfo> key = ProtoUtils.keyForProto(RetryInfo.getDefaultInstance());
    Metadata trailers = new Metadata();
    RetryInfo retryInfo =
        RetryInfo.newBuilder()
            .setRetryDelay(
                Duration.newBuilder()
                    .setNanos((int) TimeUnit.MILLISECONDS.toNanos(1L))
                    .setSeconds(0L))
            .build();
    trailers.put(key, retryInfo);
    throw Status.ABORTED
        .withDescription(
            String.format(
                "Transaction with id %s not found and has probably been aborted",
                transactionId.toStringUtf8()))
        .asRuntimeException(trailers);
  }

  private <T> void throwTransactionAborted(ByteString transactionId) {
    Metadata.Key<RetryInfo> key = ProtoUtils.keyForProto(RetryInfo.getDefaultInstance());
    Metadata trailers = new Metadata();
    RetryInfo retryInfo =
        RetryInfo.newBuilder()
            .setRetryDelay(
                Duration.newBuilder()
                    .setNanos((int) TimeUnit.MILLISECONDS.toNanos(1L))
                    .setSeconds(0L))
            .build();
    trailers.put(key, retryInfo);
    throw Status.ABORTED
        .withDescription(
            String.format("Transaction with id %s has been aborted", transactionId.toStringUtf8()))
        .asRuntimeException(trailers);
  }

  @Override
  public void read(final ReadRequest request, StreamObserver<ResultSet> responseObserver) {
    requests.add(request);
    Preconditions.checkNotNull(request.getSession());
    Session session = sessions.get(request.getSession());
    if (session == null) {
      setSessionNotFound(request.getSession(), responseObserver);
      return;
    }
    sessionLastUsed.put(session.getName(), Instant.now());
    try {
      readExecutionTime.simulateExecutionTime(exceptions, stickyGlobalExceptions, freezeLock);
      // Get or start transaction
      ByteString transactionId = getTransactionId(session, request.getTransaction());
      simulateAbort(session, transactionId);
      Iterable<String> cols = () -> request.getColumnsList().iterator();
      Statement statement =
          StatementResult.createReadStatement(
              request.getTable(),
              request.getKeySet().getAll() ? KeySet.all() : KeySet.singleKey(Key.of()),
              cols);
      StatementResult res = getResult(statement);
      returnResultSet(
          res.getResultSet(), transactionId, request.getTransaction(), responseObserver);
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
    }
  }

  @Override
  public void streamingRead(
      final ReadRequest request, StreamObserver<PartialResultSet> responseObserver) {
    requests.add(request);
    Preconditions.checkNotNull(request.getSession());
    Session session = sessions.get(request.getSession());
    if (session == null) {
      setSessionNotFound(request.getSession(), responseObserver);
      return;
    }
    sessionLastUsed.put(session.getName(), Instant.now());
    try {
      streamingReadExecutionTime.simulateExecutionTime(
          exceptions, stickyGlobalExceptions, freezeLock);
      // Get or start transaction
      ByteString transactionId = getTransactionId(session, request.getTransaction());
      if (!request.getPartitionToken().isEmpty()) {
        List<ByteString> tokens =
            partitionTokens.get(partitionKey(session.getName(), transactionId));
        if (tokens == null || !tokens.contains(request.getPartitionToken())) {
          throw Status.INVALID_ARGUMENT
              .withDescription(
                  String.format(
                      "Partition token %s is not a valid token for this transaction",
                      request.getPartitionToken()))
              .asRuntimeException();
        }
      }
      simulateAbort(session, transactionId);
      Iterable<String> cols = () -> request.getColumnsList().iterator();
      Statement statement =
          StatementResult.createReadStatement(
              request.getTable(),
              request.getKeySet().getAll() ? KeySet.all() : KeySet.singleKey(Key.of()),
              cols);
      StatementResult res = getResult(statement);
      if (res == null) {
        throw Status.NOT_FOUND
            .withDescription("No result found for " + statement.toString())
            .asRuntimeException();
      }
      returnPartialResultSet(
          res.getResultSet(),
          transactionId,
          request.getTransaction(),
          responseObserver,
          getStreamingReadExecutionTime());
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
    }
  }

  private void returnPartialResultSet(
      ResultSet resultSet,
      ByteString transactionId,
      TransactionSelector transactionSelector,
      StreamObserver<PartialResultSet> responseObserver,
      SimulatedExecutionTime executionTime) {
    ResultSetMetadata metadata = resultSet.getMetadata();
    if (transactionId == null) {
      Transaction transaction = getTemporaryTransactionOrNull(transactionSelector);
      metadata = metadata.toBuilder().setTransaction(transaction).build();
    } else {
      metadata =
          metadata
              .toBuilder()
              .setTransaction(
                  ignoreNextInlineBeginRequest.getAndSet(false)
                      ? Transaction.getDefaultInstance()
                      : Transaction.newBuilder().setId(transactionId).build())
              .build();
    }
    resultSet = resultSet.toBuilder().setMetadata(metadata).build();
    PartialResultSetsIterator iterator = new PartialResultSetsIterator(resultSet);
    long index = 0L;
    while (iterator.hasNext()) {
      SimulatedExecutionTime.checkStreamException(
          index, executionTime.exceptions, executionTime.streamIndices);
      responseObserver.onNext(iterator.next());
      index++;
    }
    responseObserver.onCompleted();
  }

  private void returnPartialResultSet(
      Session session,
      Long updateCount,
      boolean exact,
      StreamObserver<PartialResultSet> responseObserver,
      TransactionSelector transaction) {
    returnPartialResultSet(session, updateCount, exact, responseObserver, transaction, true);
  }

  private void returnPartialResultSet(
      Session session,
      Long updateCount,
      boolean exact,
      StreamObserver<PartialResultSet> responseObserver,
      TransactionSelector transaction,
      boolean complete) {
    Field field =
        Field.newBuilder()
            .setName("UPDATE_COUNT")
            .setType(Type.newBuilder().setCode(TypeCode.INT64).build())
            .build();
    if (exact) {
      responseObserver.onNext(
          PartialResultSet.newBuilder()
              .setMetadata(
                  ResultSetMetadata.newBuilder()
                      .setRowType(StructType.newBuilder().addFields(field).build())
                      .setTransaction(
                          ignoreNextInlineBeginRequest.getAndSet(false)
                              ? Transaction.getDefaultInstance()
                              : Transaction.newBuilder().setId(transaction.getId()).build())
                      .build())
              .setStats(ResultSetStats.newBuilder().setRowCountExact(updateCount).build())
              .build());
    } else {
      responseObserver.onNext(
          PartialResultSet.newBuilder()
              .setMetadata(
                  ResultSetMetadata.newBuilder()
                      .setRowType(StructType.newBuilder().addFields(field).build())
                      .setTransaction(
                          ignoreNextInlineBeginRequest.getAndSet(false)
                              ? Transaction.getDefaultInstance()
                              : Transaction.newBuilder().setId(transaction.getId()).build())
                      .build())
              .setStats(ResultSetStats.newBuilder().setRowCountLowerBound(updateCount).build())
              .build());
    }
    if (complete) {
      responseObserver.onCompleted();
    }
  }

  private boolean isPartitionedDmlTransaction(ByteString transactionId) {
    return transactionId != null
        && isPartitionedDmlTransaction.get(transactionId) != null
        && isPartitionedDmlTransaction.get(transactionId);
  }

  private boolean isReadWriteTransaction(ByteString transactionId) {
    return transactionId != null
        && transactions.get(transactionId) != null
        && transactions.get(transactionId).getReadTimestamp().getSeconds() == 0;
  }

  private ByteString getTransactionId(Session session, TransactionSelector tx) {
    ByteString transactionId = null;
    switch (tx.getSelectorCase()) {
      case SELECTOR_NOT_SET:
      case SINGLE_USE:
        transactionId = null;
        break;
      case BEGIN:
        transactionId = beginTransaction(session, tx.getBegin()).getId();
        break;
      case ID:
        Transaction transaction = transactions.get(tx.getId());
        if (transaction == null) {
          Optional<Boolean> aborted = Optional.fromNullable(abortedTransactions.get(tx.getId()));
          if (aborted.or(Boolean.FALSE)) {
            throwTransactionAborted(tx.getId());
          } else {
            throwTransactionNotFound(tx.getId());
          }
        } else {
          transactionId = transaction.getId();
          transactionLastUsed.put(transactionId, Instant.now());
        }
        break;
      default:
        throw Status.UNIMPLEMENTED.asRuntimeException();
    }
    return transactionId;
  }

  private Transaction getTemporaryTransactionOrNull(TransactionSelector tx) {
    Transaction.Builder builder;
    switch (tx.getSelectorCase()) {
      case SELECTOR_NOT_SET:
      case SINGLE_USE:
        builder = Transaction.newBuilder();
        setReadTimestamp(tx.getSingleUse(), builder);
        return builder.build();
      case BEGIN:
        builder = Transaction.newBuilder();
        setReadTimestamp(tx.getBegin(), builder);
        return builder.build();
      case ID:
        return transactions.get(tx.getId());
      default:
        return null;
    }
  }

  @Override
  public void beginTransaction(
      BeginTransactionRequest request, StreamObserver<Transaction> responseObserver) {
    requests.add(request);
    Preconditions.checkNotNull(request.getSession());
    Session session = sessions.get(request.getSession());
    if (session == null) {
      setSessionNotFound(request.getSession(), responseObserver);
      return;
    }
    sessionLastUsed.put(session.getName(), Instant.now());
    try {
      beginTransactionExecutionTime.simulateExecutionTime(
          exceptions, stickyGlobalExceptions, freezeLock);
      Transaction transaction = beginTransaction(session, request.getOptions());
      responseObserver.onNext(transaction);
      responseObserver.onCompleted();
    } catch (StatusRuntimeException t) {
      responseObserver.onError(t);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
    }
  }

  private Transaction beginTransaction(Session session, TransactionOptions options) {
    Transaction.Builder builder =
        Transaction.newBuilder().setId(generateTransactionName(session.getName()));
    if (options != null && options.getModeCase() == ModeCase.READ_ONLY) {
      setReadTimestamp(options, builder);
    }
    Transaction transaction = builder.build();
    transactions.put(transaction.getId(), transaction);
    transactionsStarted.add(transaction.getId());
    isPartitionedDmlTransaction.put(
        transaction.getId(), options.getModeCase() == ModeCase.PARTITIONED_DML);
    if (abortNextTransaction.getAndSet(false)) {
      markAbortedTransaction(transaction.getId());
    }
    return transaction;
  }

  private void setReadTimestamp(TransactionOptions options, Transaction.Builder builder) {
    if (options.getReadOnly().getStrong()) {
      builder.setReadTimestamp(getCurrentGoogleTimestamp());
    } else if (options.getReadOnly().hasReadTimestamp()) {
      builder.setReadTimestamp(options.getReadOnly().getReadTimestamp());
    } else if (options.getReadOnly().hasMinReadTimestamp()) {
      builder.setReadTimestamp(options.getReadOnly().getMinReadTimestamp());
    } else if (options.getReadOnly().hasExactStaleness()
        || options.getReadOnly().hasMaxStaleness()) {
      Timestamp timestamp = getCurrentGoogleTimestamp();
      Duration staleness =
          options.getReadOnly().hasExactStaleness()
              ? options.getReadOnly().getExactStaleness()
              : options.getReadOnly().getMaxStaleness();
      long seconds = timestamp.getSeconds() - staleness.getSeconds();
      int nanos = timestamp.getNanos() - staleness.getNanos();
      if (nanos < 0) {
        seconds = seconds - 1;
        nanos = 1000000000 + nanos;
      }
      timestamp = Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos).build();
      builder.setReadTimestamp(timestamp);
    }
  }

  private void simulateAbort(Session session, ByteString transactionId) {
    ensureMostRecentTransaction(session, transactionId);
    if (isReadWriteTransaction(transactionId)) {
      if (abortNextStatement.getAndSet(false) || abortProbability > random.nextDouble()) {
        rollbackTransaction(transactionId);
        throw createAbortedException(transactionId);
      }
    }
  }

  public StatusRuntimeException createAbortedException(ByteString transactionId) {
    RetryInfo retryInfo =
        RetryInfo.newBuilder().setRetryDelay(Duration.newBuilder().setNanos(1).build()).build();
    Metadata.Key<RetryInfo> key =
        Metadata.Key.of(
            retryInfo.getDescriptorForType().getFullName() + Metadata.BINARY_HEADER_SUFFIX,
            ProtoLiteUtils.metadataMarshaller(retryInfo));
    Metadata trailers = new Metadata();
    trailers.put(key, retryInfo);
    return Status.ABORTED
        .withDescription(
            String.format("Transaction with id %s has been aborted", transactionId.toStringUtf8()))
        .asRuntimeException(trailers);
  }

  private void ensureMostRecentTransaction(Session session, ByteString transactionId) {
    AtomicLong counter = transactionCounters.get(session.getName());
    if (transactionId != null && transactionId.toStringUtf8() != null && counter != null) {
      int index = transactionId.toStringUtf8().lastIndexOf('/');
      if (index > -1) {
        long id = Long.parseLong(transactionId.toStringUtf8().substring(index + 1));
        if (id != counter.get()) {
          throw Status.FAILED_PRECONDITION
              .withDescription(
                  String.format(
                      "This transaction has been invalidated by a later transaction in the same session.\nTransaction id: "
                          + id
                          + "\nExpected: "
                          + counter.get(),
                      session.getName()))
              .asRuntimeException();
        }
      }
    }
  }

  @Override
  public void commit(CommitRequest request, StreamObserver<CommitResponse> responseObserver) {
    requests.add(request);
    Preconditions.checkNotNull(request.getSession());
    Session session = sessions.get(request.getSession());
    if (session == null) {
      setSessionNotFound(request.getSession(), responseObserver);
      return;
    }
    sessionLastUsed.put(session.getName(), Instant.now());
    try {
      commitExecutionTime.simulateExecutionTime(exceptions, stickyGlobalExceptions, freezeLock);
      // Find or start a transaction
      Transaction transaction;
      if (request.hasSingleUseTransaction()) {
        // Start a temporary transaction
        transaction =
            beginTransaction(
                session,
                TransactionOptions.newBuilder()
                    .setReadWrite(ReadWrite.getDefaultInstance())
                    .build());
      } else if (request.getTransactionId() != null) {
        transaction = transactions.get(request.getTransactionId());
        Optional<Boolean> aborted =
            Optional.fromNullable(abortedTransactions.get(request.getTransactionId()));
        if (aborted.or(Boolean.FALSE)) {
          throwTransactionAborted(request.getTransactionId());
        }
      } else {
        // No transaction mode specified
        responseObserver.onError(
            Status.INVALID_ARGUMENT
                .withDescription("No transaction mode specified")
                .asRuntimeException());
        return;
      }
      if (transaction == null) {
        setTransactionNotFound(request.getTransactionId(), responseObserver);
        return;
      }
      simulateAbort(session, request.getTransactionId());
      commitTransaction(transaction.getId());
      CommitResponse.Builder responseBuilder =
          CommitResponse.newBuilder().setCommitTimestamp(getCurrentGoogleTimestamp());
      if (request.getReturnCommitStats()) {
        responseBuilder.setCommitStats(
            com.google.spanner.v1.CommitResponse.CommitStats.newBuilder()
                // This is not really always equal, but at least it returns a value.
                .setMutationCount(request.getMutationsCount())
                .build());
      }
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException t) {
      responseObserver.onError(t);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
    }
  }

  private void commitTransaction(ByteString transactionId) {
    transactions.remove(transactionId);
    isPartitionedDmlTransaction.remove(transactionId);
    transactionLastUsed.remove(transactionId);
  }

  @Override
  public void rollback(RollbackRequest request, StreamObserver<Empty> responseObserver) {
    requests.add(request);
    Preconditions.checkNotNull(request.getTransactionId());
    Session session = sessions.get(request.getSession());
    if (session == null) {
      setSessionNotFound(request.getSession(), responseObserver);
      return;
    }
    sessionLastUsed.put(session.getName(), Instant.now());
    try {
      rollbackExecutionTime.simulateExecutionTime(exceptions, stickyGlobalExceptions, freezeLock);
      Transaction transaction = transactions.get(request.getTransactionId());
      if (transaction != null) {
        rollbackTransaction(transaction.getId());
      }
      responseObserver.onNext(Empty.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException t) {
      responseObserver.onError(t);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
    }
  }

  void rollbackTransaction(ByteString transactionId) {
    transactions.remove(transactionId);
    isPartitionedDmlTransaction.remove(transactionId);
    transactionLastUsed.remove(transactionId);
  }

  void markAbortedTransaction(ByteString transactionId) {
    abortedTransactions.put(transactionId, Boolean.TRUE);
    transactions.remove(transactionId);
    isPartitionedDmlTransaction.remove(transactionId);
    transactionLastUsed.remove(transactionId);
  }

  @Override
  public void partitionQuery(
      PartitionQueryRequest request, StreamObserver<PartitionResponse> responseObserver) {
    requests.add(request);
    try {
      partitionQueryExecutionTime.simulateExecutionTime(
          exceptions, stickyGlobalExceptions, freezeLock);
      partition(request.getSession(), request.getTransaction(), responseObserver);
    } catch (StatusRuntimeException t) {
      responseObserver.onError(t);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
    }
  }

  @Override
  public void partitionRead(
      PartitionReadRequest request, StreamObserver<PartitionResponse> responseObserver) {
    requests.add(request);
    try {
      partitionReadExecutionTime.simulateExecutionTime(
          exceptions, stickyGlobalExceptions, freezeLock);
      partition(request.getSession(), request.getTransaction(), responseObserver);
    } catch (StatusRuntimeException t) {
      responseObserver.onError(t);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
    }
  }

  private void partition(
      String sessionName,
      TransactionSelector transactionSelector,
      StreamObserver<PartitionResponse> responseObserver) {
    Session session = sessions.get(sessionName);
    if (session == null) {
      setSessionNotFound(sessionName, responseObserver);
      return;
    }
    sessionLastUsed.put(session.getName(), Instant.now());
    try {
      ByteString transactionId = getTransactionId(session, transactionSelector);
      responseObserver.onNext(
          PartitionResponse.newBuilder()
              .addPartitions(
                  Partition.newBuilder()
                      .setPartitionToken(generatePartitionToken(session.getName(), transactionId))
                      .build())
              .build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    } catch (Throwable t) {
      responseObserver.onError(Status.INTERNAL.asRuntimeException());
    }
  }

  public int numSessionsCreated() {
    return numSessionsCreated.get();
  }

  @Override
  public List<AbstractMessage> getRequests() {
    return new ArrayList<>(this.requests);
  }

  public void clearRequests() {
    this.requests.clear();
  }

  @SuppressWarnings("unchecked")
  public <T extends AbstractMessage> List<T> getRequestsOfType(Class<T> type) {
    List<T> result = new ArrayList<>();
    for (AbstractMessage message : this.requests) {
      if (message.getClass().equals(type)) {
        result.add((T) message);
      }
    }
    return result;
  }

  public Iterable<Class<? extends AbstractMessage>> getRequestTypes() {
    List<Class<? extends AbstractMessage>> res = new LinkedList<>();
    for (AbstractMessage m : this.requests) {
      res.add(m.getClass());
    }
    return res;
  }

  public int countRequestsOfType(Class<? extends AbstractMessage> type) {
    int c = 0;
    for (AbstractMessage m : this.requests) {
      if (m.getClass().equals(type)) {
        c++;
      }
    }
    return c;
  }

  public void waitForLastRequestToBe(Class<? extends AbstractMessage> type, long timeoutMillis)
      throws InterruptedException, TimeoutException {
    Stopwatch watch = Stopwatch.createStarted();
    while (!(this.requests.peekLast() != null
        && this.requests.peekLast().getClass().equals(type))) {
      Thread.sleep(1L);
      if (watch.elapsed(TimeUnit.MILLISECONDS) > timeoutMillis) {
        throw new TimeoutException(
            "Timeout while waiting for last request to become " + type.getName());
      }
    }
  }

  public List<ByteString> getTransactionsStarted() {
    return new ArrayList<>(transactionsStarted);
  }

  public void waitForRequestsToContain(Class<? extends AbstractMessage> type, long timeoutMillis)
      throws InterruptedException, TimeoutException {
    Stopwatch watch = Stopwatch.createStarted();
    while (countRequestsOfType(type) == 0) {
      Thread.sleep(1L);
      if (watch.elapsed(TimeUnit.MILLISECONDS) > timeoutMillis) {
        throw new TimeoutException(
            "Timeout while waiting for requests to contain " + type.getName());
      }
    }
  }

  public void waitForRequestsToContain(
      Predicate<? super AbstractMessage> predicate, long timeoutMillis)
      throws InterruptedException, TimeoutException {
    Stopwatch watch = Stopwatch.createStarted();
    while (true) {
      Iterable<AbstractMessage> msg = Iterables.filter(getRequests(), predicate);
      if (msg.iterator().hasNext()) {
        break;
      }
      Thread.sleep(1L);
      if (watch.elapsed(TimeUnit.MILLISECONDS) > timeoutMillis) {
        throw new TimeoutException(
            "Timeout while waiting for requests to contain the wanted request");
      }
    }
  }

  @Override
  public void addResponse(AbstractMessage response) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addException(Exception exception) {
    exceptions.add(exception);
  }

  public void clearExceptions() {
    exceptions.clear();
  }

  public void setStickyGlobalExceptions(boolean sticky) {
    this.stickyGlobalExceptions = sticky;
  }

  @Override
  public ServerServiceDefinition getServiceDefinition() {
    return bindService();
  }

  /** Removes all sessions and transactions. Mocked results are not removed. */
  @Override
  public void reset() {
    requests = new ConcurrentLinkedDeque<>();
    exceptions = new ConcurrentLinkedQueue<>();
    statementGetCounts = new ConcurrentHashMap<>();
    sessions = new ConcurrentHashMap<>();
    sessionLastUsed = new ConcurrentHashMap<>();
    transactions = new ConcurrentHashMap<>();
    transactionsStarted.clear();
    isPartitionedDmlTransaction = new ConcurrentHashMap<>();
    abortedTransactions = new ConcurrentHashMap<>();
    transactionCounters = new ConcurrentHashMap<>();
    partitionTokens = new ConcurrentHashMap<>();
    transactionLastUsed = new ConcurrentHashMap<>();

    numSessionsCreated.set(0);
    stickyGlobalExceptions = false;
    freezeLock.countDown();
  }

  public void removeAllExecutionTimes() {
    batchCreateSessionsExecutionTime = NO_EXECUTION_TIME;
    beginTransactionExecutionTime = NO_EXECUTION_TIME;
    commitExecutionTime = NO_EXECUTION_TIME;
    createSessionExecutionTime = NO_EXECUTION_TIME;
    deleteSessionExecutionTime = NO_EXECUTION_TIME;
    executeBatchDmlExecutionTime = NO_EXECUTION_TIME;
    executeSqlExecutionTime = NO_EXECUTION_TIME;
    executeStreamingSqlExecutionTime = NO_EXECUTION_TIME;
    getSessionExecutionTime = NO_EXECUTION_TIME;
    listSessionsExecutionTime = NO_EXECUTION_TIME;
    partitionQueryExecutionTime = NO_EXECUTION_TIME;
    partitionReadExecutionTime = NO_EXECUTION_TIME;
    readExecutionTime = NO_EXECUTION_TIME;
    rollbackExecutionTime = NO_EXECUTION_TIME;
    streamingReadExecutionTime = NO_EXECUTION_TIME;
  }

  public SimulatedExecutionTime getBeginTransactionExecutionTime() {
    return beginTransactionExecutionTime;
  }

  public void setBeginTransactionExecutionTime(
      SimulatedExecutionTime beginTransactionExecutionTime) {
    this.beginTransactionExecutionTime = Preconditions.checkNotNull(beginTransactionExecutionTime);
  }

  public SimulatedExecutionTime getCommitExecutionTime() {
    return commitExecutionTime;
  }

  public void setCommitExecutionTime(SimulatedExecutionTime commitExecutionTime) {
    this.commitExecutionTime = Preconditions.checkNotNull(commitExecutionTime);
  }

  public SimulatedExecutionTime getBatchCreateSessionsExecutionTime() {
    return batchCreateSessionsExecutionTime;
  }

  public void setBatchCreateSessionsExecutionTime(
      SimulatedExecutionTime batchCreateSessionsExecutionTime) {
    this.batchCreateSessionsExecutionTime =
        Preconditions.checkNotNull(batchCreateSessionsExecutionTime);
  }

  public SimulatedExecutionTime getCreateSessionExecutionTime() {
    return createSessionExecutionTime;
  }

  public void setCreateSessionExecutionTime(SimulatedExecutionTime createSessionExecutionTime) {
    this.createSessionExecutionTime = Preconditions.checkNotNull(createSessionExecutionTime);
  }

  public SimulatedExecutionTime getDeleteSessionExecutionTime() {
    return deleteSessionExecutionTime;
  }

  public void setDeleteSessionExecutionTime(SimulatedExecutionTime deleteSessionExecutionTime) {
    this.deleteSessionExecutionTime = Preconditions.checkNotNull(deleteSessionExecutionTime);
  }

  public SimulatedExecutionTime getExecuteBatchDmlExecutionTime() {
    return executeBatchDmlExecutionTime;
  }

  public void setExecuteBatchDmlExecutionTime(SimulatedExecutionTime executeBatchDmlExecutionTime) {
    this.executeBatchDmlExecutionTime = Preconditions.checkNotNull(executeBatchDmlExecutionTime);
  }

  public SimulatedExecutionTime getExecuteSqlExecutionTime() {
    return executeSqlExecutionTime;
  }

  public void setExecuteSqlExecutionTime(SimulatedExecutionTime executeSqlExecutionTime) {
    this.executeSqlExecutionTime = Preconditions.checkNotNull(executeSqlExecutionTime);
  }

  public SimulatedExecutionTime getExecuteStreamingSqlExecutionTime() {
    return executeStreamingSqlExecutionTime;
  }

  public void setExecuteStreamingSqlExecutionTime(
      SimulatedExecutionTime executeStreamingSqlExecutionTime) {
    this.executeStreamingSqlExecutionTime =
        Preconditions.checkNotNull(executeStreamingSqlExecutionTime);
  }

  public SimulatedExecutionTime getGetSessionExecutionTime() {
    return getSessionExecutionTime;
  }

  public void setGetSessionExecutionTime(SimulatedExecutionTime getSessionExecutionTime) {
    this.getSessionExecutionTime = Preconditions.checkNotNull(getSessionExecutionTime);
  }

  public SimulatedExecutionTime getListSessionsExecutionTime() {
    return listSessionsExecutionTime;
  }

  public void setListSessionsExecutionTime(SimulatedExecutionTime listSessionsExecutionTime) {
    this.listSessionsExecutionTime = Preconditions.checkNotNull(listSessionsExecutionTime);
  }

  public SimulatedExecutionTime getPartitionQueryExecutionTime() {
    return partitionQueryExecutionTime;
  }

  public void setPartitionQueryExecutionTime(SimulatedExecutionTime partitionQueryExecutionTime) {
    this.partitionQueryExecutionTime = Preconditions.checkNotNull(partitionQueryExecutionTime);
  }

  public SimulatedExecutionTime getPartitionReadExecutionTime() {
    return partitionReadExecutionTime;
  }

  public void setPartitionReadExecutionTime(SimulatedExecutionTime partitionReadExecutionTime) {
    this.partitionReadExecutionTime = Preconditions.checkNotNull(partitionReadExecutionTime);
  }

  public SimulatedExecutionTime getReadExecutionTime() {
    return readExecutionTime;
  }

  public void setReadExecutionTime(SimulatedExecutionTime readExecutionTime) {
    this.readExecutionTime = Preconditions.checkNotNull(readExecutionTime);
  }

  public SimulatedExecutionTime getRollbackExecutionTime() {
    return rollbackExecutionTime;
  }

  public void setRollbackExecutionTime(SimulatedExecutionTime rollbackExecutionTime) {
    this.rollbackExecutionTime = Preconditions.checkNotNull(rollbackExecutionTime);
  }

  public SimulatedExecutionTime getStreamingReadExecutionTime() {
    return streamingReadExecutionTime;
  }

  public void setStreamingReadExecutionTime(SimulatedExecutionTime streamingReadExecutionTime) {
    this.streamingReadExecutionTime = Preconditions.checkNotNull(streamingReadExecutionTime);
  }
}
