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

import static com.google.common.base.Preconditions.checkState;

import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.ServerStream;
import com.google.api.gax.rpc.UnavailableException;
import com.google.cloud.spanner.SessionImpl.SessionTransaction;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.protobuf.ByteString;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryMode;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.Transaction;
import com.google.spanner.v1.TransactionOptions;
import com.google.spanner.v1.TransactionSelector;
import io.grpc.Status.Code;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Partitioned DML transaction for bulk updates and deletes. */
class PartitionedDMLTransaction implements SessionTransaction {
  private static final Logger log = Logger.getLogger(PartitionedDMLTransaction.class.getName());

  private final SessionImpl session;
  private final SpannerRpc rpc;
  private volatile boolean isValid = true;

  PartitionedDMLTransaction(SessionImpl session, SpannerRpc rpc) {
    this.session = session;
    this.rpc = rpc;
  }

  private ByteString initTransaction() {
    final BeginTransactionRequest request =
        BeginTransactionRequest.newBuilder()
            .setSession(session.getName())
            .setOptions(
                TransactionOptions.newBuilder()
                    .setPartitionedDml(TransactionOptions.PartitionedDml.getDefaultInstance()))
            .build();
    Transaction txn = rpc.beginTransaction(request, session.getOptions());
    if (txn.getId().isEmpty()) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INTERNAL,
          "Failed to init transaction, missing transaction id\n" + session.getName());
    }
    return txn.getId();
  }

  /**
   * Executes the {@link Statement} using a partitioned dml transaction with automatic retry if the
   * transaction was aborted. The update method uses the ExecuteStreamingSql RPC to execute the
   * statement, and will retry the stream if an {@link UnavailableException} is thrown, using the
   * last seen resume token if the server returns any.
   */
  long executeStreamingPartitionedUpdate(final Statement statement) {
    checkState(isValid, "Partitioned DML has been invalidated by a new operation on the session");
    log.log(Level.FINER, "Starting PartitionedUpdate statement");
    boolean foundStats = false;
    long updateCount = 0L;
    long streams = 0L;
    try {
      // Loop to catch AbortedExceptions.
      while (true) {
        ByteString resumeToken = ByteString.EMPTY;
        try {
          ByteString transactionId = initTransaction();
          final ExecuteSqlRequest.Builder builder =
              ExecuteSqlRequest.newBuilder()
                  .setSql(statement.getSql())
                  .setQueryMode(QueryMode.NORMAL)
                  .setSession(session.getName())
                  .setTransaction(TransactionSelector.newBuilder().setId(transactionId).build());
          Map<String, Value> stmtParameters = statement.getParameters();
          if (!stmtParameters.isEmpty()) {
            com.google.protobuf.Struct.Builder paramsBuilder = builder.getParamsBuilder();
            for (Map.Entry<String, Value> param : stmtParameters.entrySet()) {
              paramsBuilder.putFields(param.getKey(), param.getValue().toProto());
              builder.putParamTypes(param.getKey(), param.getValue().getType().toProto());
            }
          }
          while (true) {
            try {
              builder.setResumeToken(resumeToken);
              ServerStream<PartialResultSet> stream =
                  rpc.executeStreamingPartitionedDml(builder.build(), session.getOptions());
              for (PartialResultSet rs : stream) {
                if (rs.getResumeToken() != null && !ByteString.EMPTY.equals(rs.getResumeToken())) {
                  resumeToken = rs.getResumeToken();
                }
                streams++;
                log.log(
                    Level.FINER,
                    "processing stream #"
                        + streams
                        + ", current resume token is "
                        + resumeToken.toStringUtf8());
                if (rs.hasStats()) {
                  foundStats = true;
                  updateCount += rs.getStats().getRowCountLowerBound();
                }
              }
              break;
            } catch (UnavailableException e) {
              // Retry the stream in the same transaction if the stream breaks with
              // UnavailableException and we have a resume token. Otherwise, we just retry the
              // entire transaction.
              if (resumeToken != null && !ByteString.EMPTY.equals(resumeToken)) {
                log.log(
                    Level.WARNING,
                    "Retrying PartitionedDml stream using resume token '"
                        + resumeToken.toStringUtf8()
                        + "' because of broken stream",
                    e);
              } else {
                throw new com.google.api.gax.rpc.AbortedException(
                    e, GrpcStatusCode.of(Code.ABORTED), true);
              }
            }
          }
          break;
        } catch (com.google.api.gax.rpc.AbortedException e) {
          // Retry using a new transaction but with the same session if the transaction is aborted.
          log.log(Level.WARNING, "Retrying PartitionedDml transaction after AbortedException", e);
        }
      }
      if (!foundStats) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT,
            "Partitioned DML response missing stats possibly due to non-DML statement as input");
      }
      log.log(Level.FINER, "Finished PartitionedUpdate statement");
      return updateCount;
    } catch (Exception e) {
      throw SpannerExceptionFactory.newSpannerException(e);
    }
  }

  @Override
  public void invalidate() {
    isValid = false;
  }
}
