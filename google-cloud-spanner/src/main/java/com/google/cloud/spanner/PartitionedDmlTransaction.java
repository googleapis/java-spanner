/*
 * Copyright 2020 Google LLC
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

import static com.google.common.base.Preconditions.checkState;

import com.google.api.core.InternalApi;
import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.AbortedException;
import com.google.api.gax.rpc.DeadlineExceededException;
import com.google.api.gax.rpc.InternalException;
import com.google.api.gax.rpc.ServerStream;
import com.google.api.gax.rpc.UnavailableException;
import com.google.cloud.spanner.Options.UpdateOption;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.protobuf.ByteString;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.RequestOptions;
import com.google.spanner.v1.Transaction;
import com.google.spanner.v1.TransactionOptions;
import com.google.spanner.v1.TransactionSelector;
import io.grpc.Status;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@InternalApi
public class PartitionedDmlTransaction implements SessionImpl.SessionTransaction {

  private static final Logger LOGGER = Logger.getLogger(PartitionedDmlTransaction.class.getName());

  private final SessionImpl session;
  private final SpannerRpc rpc;
  private final Ticker ticker;
  private final IsRetryableInternalError isRetryableInternalErrorPredicate;
  private volatile boolean isValid = true;

  PartitionedDmlTransaction(SessionImpl session, SpannerRpc rpc, Ticker ticker) {
    this.session = session;
    this.rpc = rpc;
    this.ticker = ticker;
    this.isRetryableInternalErrorPredicate = new IsRetryableInternalError();
  }

  /**
   * Executes the {@link Statement} using a partitioned dml transaction with automatic retry if the
   * transaction was aborted. The update method uses the ExecuteStreamingSql RPC to execute the
   * statement, and will retry the stream if an {@link UnavailableException} is thrown, using the
   * last seen resume token if the server returns any.
   */
  long executeStreamingPartitionedUpdate(
      final Statement statement, final Duration timeout, final UpdateOption... updateOptions) {
    checkState(isValid, "Partitioned DML has been invalidated by a new operation on the session");
    LOGGER.log(Level.FINER, "Starting PartitionedUpdate statement");

    ByteString resumeToken = ByteString.EMPTY;
    boolean foundStats = false;
    long updateCount = 0L;
    Stopwatch stopwatch = Stopwatch.createStarted(ticker);
    XGoogSpannerRequestId reqId =
        session.getRequestIdCreator().nextRequestId(session.getChannel(), 1);
    UpdateOption[] allOptions = new UpdateOption[updateOptions.length + 1];
    System.arraycopy(updateOptions, 0, allOptions, 0, updateOptions.length);
    allOptions[allOptions.length - 1] = new Options.RequestIdOption(reqId);
    Options options = Options.fromUpdateOptions(allOptions);

    try {
      ExecuteSqlRequest request = newTransactionRequestFrom(statement, options);

      while (true) {
        final Duration remainingTimeout = tryUpdateTimeout(timeout, stopwatch);

        try {
          System.out.println("\033[31mreqIdPump: " + reqId + "\033[00m");
          ServerStream<PartialResultSet> stream =
              rpc.executeStreamingPartitionedDml(
                  request, reqId.withOptions(session.getOptions()), remainingTimeout);

          for (PartialResultSet rs : stream) {
            if (rs.getResumeToken() != null && !rs.getResumeToken().isEmpty()) {
              resumeToken = rs.getResumeToken();
            }
            if (rs.hasStats()) {
              foundStats = rs.getStats().hasRowCountLowerBound();
              updateCount += rs.getStats().getRowCountLowerBound();
            }
          }
          break;
        } catch (UnavailableException e) {
          LOGGER.log(
              Level.FINER, "Retrying PartitionedDml transaction after UnavailableException", e);
          reqId.incrementAttempt();
          request = resumeOrRestartRequest(resumeToken, statement, request, options);
        } catch (InternalException e) {
          if (!isRetryableInternalErrorPredicate.apply(e)) {
            throw e;
          }

          LOGGER.log(
              Level.FINER, "Retrying PartitionedDml transaction after InternalException - EOS", e);
          reqId.incrementAttempt();
          request = resumeOrRestartRequest(resumeToken, statement, request, options);
        } catch (AbortedException e) {
          LOGGER.log(Level.FINER, "Retrying PartitionedDml transaction after AbortedException", e);
          resumeToken = ByteString.EMPTY;
          foundStats = false;
          updateCount = 0L;
          request = newTransactionRequestFrom(statement, options);
          // Create a new xGoogSpannerRequestId.
          reqId = session.getRequestIdCreator().nextRequestId(session.getChannel(), 1);
        } catch (SpannerException e) {
          e.setRequestId(reqId);
          throw e;
        }
      }
      if (!foundStats) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT,
            "Partitioned DML response missing stats possibly due to non-DML statement as input",
            reqId);
      }
      LOGGER.log(Level.FINER, "Finished PartitionedUpdate statement");
      return updateCount;
    } catch (Exception e) {
      throw SpannerExceptionFactory.newSpannerException(e, reqId);
    }
  }

  @Override
  public void invalidate() {
    isValid = false;
  }

  /** No-op method needed to implement SessionTransaction interface. */
  @Override
  public void setSpan(ISpan span) {}

  /** No-op method needed to implement SessionTransaction interface. */
  @Override
  public void close() {}

  private Duration tryUpdateTimeout(final Duration timeout, final Stopwatch stopwatch) {
    final Duration remainingTimeout =
        timeout.minus(stopwatch.elapsed(TimeUnit.MILLISECONDS), ChronoUnit.MILLIS);
    if (remainingTimeout.isNegative() || remainingTimeout.isZero()) {
      // The total deadline has been exceeded while retrying.
      throw new DeadlineExceededException(
          null, GrpcStatusCode.of(Status.Code.DEADLINE_EXCEEDED), false);
    }
    return remainingTimeout;
  }

  private ExecuteSqlRequest resumeOrRestartRequest(
      final ByteString resumeToken,
      final Statement statement,
      final ExecuteSqlRequest originalRequest,
      final Options options) {
    if (resumeToken.isEmpty()) {
      return newTransactionRequestFrom(statement, options);
    } else {
      return ExecuteSqlRequest.newBuilder(originalRequest).setResumeToken(resumeToken).build();
    }
  }

  @VisibleForTesting
  ExecuteSqlRequest newTransactionRequestFrom(final Statement statement, final Options options) {
    ByteString transactionId = initTransaction(options);

    final TransactionSelector transactionSelector =
        TransactionSelector.newBuilder().setId(transactionId).build();
    final ExecuteSqlRequest.Builder builder =
        ExecuteSqlRequest.newBuilder()
            .setSql(statement.getSql())
            .setQueryMode(ExecuteSqlRequest.QueryMode.NORMAL)
            .setSession(session.getName())
            .setTransaction(transactionSelector);

    setParameters(builder, statement.getParameters());

    builder.setResumeToken(ByteString.EMPTY);

    if (options.hasPriority() || options.hasTag()) {
      RequestOptions.Builder requestOptionsBuilder = RequestOptions.newBuilder();
      if (options.hasPriority()) {
        requestOptionsBuilder.setPriority(options.priority());
      }
      if (options.hasTag()) {
        requestOptionsBuilder.setRequestTag(options.tag());
      }
      builder.setRequestOptions(requestOptionsBuilder.build());
    }
    return builder.build();
  }

  private ByteString initTransaction(final Options options) {
    final BeginTransactionRequest request =
        BeginTransactionRequest.newBuilder()
            .setSession(session.getName())
            .setOptions(
                TransactionOptions.newBuilder()
                    .setPartitionedDml(TransactionOptions.PartitionedDml.getDefaultInstance())
                    .setExcludeTxnFromChangeStreams(
                        options.withExcludeTxnFromChangeStreams() == Boolean.TRUE))
            .build();
    XGoogSpannerRequestId reqId =
        session.getRequestIdCreator().nextRequestId(session.getChannel(), 1);
    Transaction tx = rpc.beginTransaction(request, reqId.withOptions(session.getOptions()), true);
    if (tx.getId().isEmpty()) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INTERNAL,
          "Failed to init transaction, missing transaction id\n" + session.getName(),
          reqId);
    }
    return tx.getId();
  }

  private void setParameters(
      final ExecuteSqlRequest.Builder requestBuilder,
      final Map<String, Value> statementParameters) {
    AbstractReadContext.addParameters(requestBuilder, statementParameters);
  }
}
