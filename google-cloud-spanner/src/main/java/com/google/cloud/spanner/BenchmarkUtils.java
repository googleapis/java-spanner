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

import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * This class is to be used only for benchmarking, it should NOT be used in production.
 */
public class BenchmarkUtils {

  private static final Logger LOGGER = Logger.getLogger(BenchmarkUtils.class.getName());

  /**
   * This is to be used in benchmarking only. There is no need to do this in production code.
   *
   * <p>Blocks until the session pool is ready. This entails all of:
   *
   * <p>
   *
   * <ul>
   *   <li>Waiting for all the session to be created in the session pool.
   *   <li>Waiting for all the write transactions to be prepared in the session pool.
   * </ul>
   *
   * <p>Example to use this.
   *
   * <pre>{@code
   * String projectId = "my_project_id";
   * String instanceId = "my_instance_id";
   * String databaseId = "my_database_id";
   *
   * final SpannerOptions options = SpannerOptions.newBuilder().build();
   * final Spanner spanner = options.getService();
   * final DatabaseId id = DatabaseId.of(projectId, instanceId, databaseId);
   * final DatabaseClient databaseClient = spanner.getDatabaseClient(id);
   *
   * // Blocks until session pool is ready
   * BenchmarkUtils.waitForSessionPoolToBeReady(databaseClient, options.getSessionPoolOptions());
   * // Code after here, can assume sessions have already been prepared.
   * }</pre>
   */
  public static void waitForSessionPoolToBeReady(
      final DatabaseClient databaseClient, final SessionPoolOptions sessionPoolOptions)
      throws InterruptedException {
    final DatabaseClientImpl databaseClientImpl = (DatabaseClientImpl) databaseClient;

    // Verifies that the database exists
    verifyDatabaseExist(databaseClientImpl);

    // Busy waits until the session pool is ready AND the write sessions are prepared.
    waitUntilSessionPoolReady(
        databaseClientImpl.pool,
        sessionPoolOptions.getMinSessions(),
        (int)
            Math.floor(
                sessionPoolOptions.getMinSessions()
                    * sessionPoolOptions.getWriteSessionsFraction()));
  }

  /**
   * Throws an exception if the project / instance / database does NOT exist.
   */
  private static void verifyDatabaseExist(DatabaseClient databaseClient) {
    databaseClient
        .readWriteTransaction()
        .run(
            new TransactionCallable<Boolean>() {
              @Nullable
              @Override
              public Boolean run(TransactionContext transaction) throws Exception {
                transaction.executeQuery(Statement.of("SELECT 1"));
                return true;
              }
            });
  }

  /**
   * Busy waits until the session pool is ready AND the write sessions are prepared.
   */
  private static void waitUntilSessionPoolReady(
      final SessionPool sessionPool,
      final int requiredTotalSessions,
      final int requiredWriteSessions)
      throws InterruptedException {

    long start = System.currentTimeMillis();
    while ((sessionPool.getNumberOfSessionsInPool() < requiredTotalSessions)
        && (sessionPool.getNumberOfWriteSessionsInPool() < requiredWriteSessions)) {
      Thread.sleep(100L);
    }
    long end = System.currentTimeMillis();

    final int totalSessions = sessionPool.totalSessions();
    final int writeSessions = sessionPool.getNumberOfWriteSessionsInPool();
    final int readSessions = totalSessions - writeSessions;
    LOGGER.log(Level.FINER, "Session pool ready in " + (end - start) + " ms");
    LOGGER.log(
        Level.FINER,
        "There are "
            + totalSessions
            + " sessions in the pool ("
            + writeSessions
            + " write sessions / "
            + readSessions
            + " read sessions)");
  }
}
