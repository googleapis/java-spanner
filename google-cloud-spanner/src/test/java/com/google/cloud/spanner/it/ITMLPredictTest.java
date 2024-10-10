/*
 * Copyright 2017 Google LLC
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

package com.google.cloud.spanner.it;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.IntegrationTestEnv;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ParallelIntegrationTest;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.TransactionRunner;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.cloud.spanner.connection.ConnectionOptions;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Integration tests for ML.PREDICT Function.
 *
 * It uses textembedding-gecko endpoints for computing the embeddings using ML.PREDICT function.
 */
@Category(ParallelIntegrationTest.class)
public final class ITMLPredictTest {
  @ClassRule public static IntegrationTestEnv env = new IntegrationTestEnv();
  private static DatabaseClient gSQLClient;

  private static final int TEST_ROW_COUNT = 10;

  private static boolean throwAbortOnce = false;

  @BeforeClass
  public static void setUpDatabase() {
    Database googleStandardSQLDatabase =
        env.getTestHelper()
            .createTestDatabase(
                "CREATE TABLE T ("
                    + " K INT64 NOT NULL,"
                    + " V STRING(MAX),"
                    + " gecko_001_embedding ARRAY<FLOAT64>)"
                    + " PRIMARY KEY (K)",
                "CREATE MODEL GECKO INPUT (content STRING(MAX)) OUTPUT (embeddings"
                    + " STRUCT<statistics STRUCT<truncated BOOL, token_count FLOAT64>, values"
                    + " ARRAY<FLOAT64>>) REMOTE OPTIONS ( endpoint ="
                    + getModelEndpoint()
                    + " )");
    gSQLClient = env.getTestHelper().getDatabaseClient(googleStandardSQLDatabase);
  }

  @AfterClass
  public static void teardown() {
    ConnectionOptions.closeSpanner();
  }

  @Before
  public void cleanUpTestData() {
    gSQLClient.writeAtLeastOnce(Collections.singletonList(Mutation.delete("T", KeySet.all())));
  }

  private static String getModelEndpoint() {
    String projectId = env.getTestHelper().getOptions().getDefaultProjectId();
    return "'https://us-central1-aiplatform.googleapis.com/v1/projects/"
        + projectId
        + "/locations/us-central1/publishers/google/models/textembedding-gecko@002'";
  }

  private String getInsertDml() {
    String rows =
        IntStream.range(0, TEST_ROW_COUNT)
            .mapToObj(i -> "(" + i + ", 'HelloWorld')")
            .collect(Collectors.joining(", "));
    String dml = "INSERT INTO T (k, v) VALUES " + rows;
    return dml;
  }

  private String getUpdateEmbeddingDml() {
    return "UPDATE T SET T.gecko_001_embedding = (SELECT embeddings.values FROM ML.PREDICT(MODEL"
        + " GECKO, (SELECT T.V AS CONTENT))"
        + "@{remote_udf_max_rows_per_rpc=5,remote_udf_max_buffered_rows=500})"
        + " WHERE TRUE";
  }

  private String getSelectEmbeddingSql() {
    return "SELECT embeddings.values FROM ML.PREDICT(MODEL"
        + " GECKO, (SELECT T.V AS CONTENT FROM T))"
        + "@{remote_udf_max_rows_per_rpc=5,remote_udf_max_buffered_rows=500}";
  }

  private void executeUpdate(long expectedCount, final String... stmts) {
    final TransactionCallable<Long> callable =
        transaction -> {
          long rowCount = 0;
          for (String stmt : stmts) {
            if (throwAbortOnce) {
              throwAbortOnce = false;
              throw SpannerExceptionFactory.newSpannerException(ErrorCode.ABORTED, "Abort in test");
            }
            rowCount += transaction.executeUpdate(Statement.of(stmt));
          }
          return rowCount;
        };

    TransactionRunner runner = gSQLClient.readWriteTransaction();
    Long rowCount = runner.run(callable);
    assertThat(rowCount).isEqualTo(expectedCount);
  }

  @Test
  public void mlPredictUpdateSuccess() {
    executeUpdate(TEST_ROW_COUNT, getInsertDml());
    executeUpdate(TEST_ROW_COUNT, getUpdateEmbeddingDml());
  }

  @Test
  public void abortOnceShouldSucceedAfterRetry() {
    try {
      executeUpdate(TEST_ROW_COUNT, getInsertDml());
      throwAbortOnce = true;
      executeUpdate(TEST_ROW_COUNT, getUpdateEmbeddingDml());
      assertThat(throwAbortOnce).isFalse();
    } catch (AbortedException e) {
      fail("Abort Exception not caught and retried");
    }
  }

  @Test
  public void mlPredictWithpartitionedDML() {
    executeUpdate(TEST_ROW_COUNT, getInsertDml());

    long pdmlUpdatedRowCount =
        gSQLClient.executePartitionedUpdate(Statement.of(getUpdateEmbeddingDml()));
    // Note: With PDML there is a possibility of network replay or partial update to occur, causing
    // this assert to fail. We should remove this assert if it is a recurring failure in IT tests.
    assertThat(pdmlUpdatedRowCount).isEqualTo(TEST_ROW_COUNT);
  }

  @Test
  public void mlPredictWithinSelectQuery() {
    executeUpdate(TEST_ROW_COUNT, getInsertDml());

    Statement queryStatement = Statement.of(getSelectEmbeddingSql());

    try (ResultSet resultSet =
        queryStatement.executeQuery(gSQLClient.singleUse(TimestampBound.strong()))) {
      assertThat(resultSet.next()).isTrue();
      assertThat(resultSet.getColumnCount()).isEqualTo(1);
      assertThat(resultSet.getDoubleList(0).size()).isEqualTo(768);
    } catch (Exception e) {
      fail("Failed due to exception " +  e.getMessage());
    }
  }
}
