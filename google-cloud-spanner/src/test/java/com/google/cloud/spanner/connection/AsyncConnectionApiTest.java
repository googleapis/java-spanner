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

package com.google.cloud.spanner.connection;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.core.ApiFuture;
import com.google.cloud.spanner.AsyncResultSet;
import com.google.cloud.spanner.AsyncResultSet.CallbackResponse;
import com.google.cloud.spanner.AsyncResultSet.ReadyCallback;
import com.google.common.base.Function;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AsyncConnectionApiTest extends AbstractMockServerTest {
  private static final ExecutorService executor = Executors.newSingleThreadExecutor();

  @AfterClass
  public static void stopExecutor() {
    executor.shutdown();
  }

  @Test
  public void testSimpleSelectAutocommit() throws Exception {
    testSimpleSelect(
        new Function<AsyncConnection, Void>() {
          @Override
          public Void apply(AsyncConnection input) {
            input.setAutocommit(true);
            return null;
          }
        });
  }

  @Test
  public void testSimpleSelectReadOnly() throws Exception {
    testSimpleSelect(
        new Function<AsyncConnection, Void>() {
          @Override
          public Void apply(AsyncConnection input) {
            input.setReadOnly(true);
            return null;
          }
        });
  }

  @Test
  public void testSimpleSelectReadWrite() throws Exception {
    testSimpleSelect(
        new Function<AsyncConnection, Void>() {
          @Override
          public Void apply(AsyncConnection input) {
            return null;
          }
        });
  }

  private void testSimpleSelect(Function<AsyncConnection, Void> connectionConfigurator)
      throws Exception {
    final AtomicInteger rowCount = new AtomicInteger();
    ApiFuture<Void> res;
    try (AsyncConnection connection = createAsyncConnection()) {
      connectionConfigurator.apply(connection);
      // Verify that the call is non-blocking.
      mockSpanner.freeze();
      try (AsyncResultSet rs = connection.executeQueryAsync(SELECT_RANDOM_STATEMENT)) {
        mockSpanner.unfreeze();
        res =
            rs.setCallback(
                executor,
                new ReadyCallback() {
                  @Override
                  public CallbackResponse cursorReady(AsyncResultSet resultSet) {
                    while (true) {
                      switch (resultSet.tryNext()) {
                        case OK:
                          rowCount.incrementAndGet();
                          break;
                        case DONE:
                          return CallbackResponse.DONE;
                        case NOT_READY:
                          return CallbackResponse.CONTINUE;
                      }
                    }
                  }
                });
      }
      res.get();
      assertThat(rowCount.get()).isEqualTo(RANDOM_RESULT_SET_ROW_COUNT);
    }
  }
}
