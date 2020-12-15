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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.gax.core.ExecutorProvider;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.ExecuteSqlRequest.QueryMode;
import com.google.spanner.v1.ExecuteSqlRequest.QueryOptions;
import com.google.spanner.v1.TransactionSelector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AbstractReadContextTest {
  @Parameter(0)
  public QueryOptions defaultQueryOptions;

  @Parameters(name = "SpannerOptions.DefaultQueryOptions = {0}")
  public static Collection<Object[]> parameters() {
    List<Object[]> params = new ArrayList<>();
    params.add(new Object[] {QueryOptions.getDefaultInstance()});
    params.add(
        new Object[] {QueryOptions.newBuilder().setOptimizerVersion("some-version").build()});
    return params;
  }

  class TestReadContextBuilder
      extends AbstractReadContext.Builder<TestReadContextBuilder, TestReadContext> {
    @Override
    TestReadContext build() {
      return new TestReadContext(this);
    }
  }

  private final class TestReadContext extends AbstractReadContext {
    TestReadContext(TestReadContextBuilder builder) {
      super(builder);
    }

    @Override
    TransactionSelector getTransactionSelector() {
      return TransactionSelector.getDefaultInstance();
    }
  }

  private TestReadContext context;

  @Before
  public void setup() {
    SessionImpl session = mock(SessionImpl.class);
    when(session.getName()).thenReturn("session-1");
    TestReadContextBuilder builder = new TestReadContextBuilder();
    context =
        builder
            .setSession(session)
            .setRpc(mock(SpannerRpc.class))
            .setDefaultQueryOptions(defaultQueryOptions)
            .setExecutorProvider(mock(ExecutorProvider.class))
            .build();
  }

  @Test
  public void executeSqlRequestBuilderWithoutQueryOptions() {
    ExecuteSqlRequest request =
        context
            .getExecuteSqlRequestBuilder(
                Statement.of("SELECT FOO FROM BAR"), QueryMode.NORMAL, Options.fromQueryOptions())
            .build();
    assertThat(request.getSql()).isEqualTo("SELECT FOO FROM BAR");
    assertThat(request.getQueryOptions()).isEqualTo(defaultQueryOptions);
  }

  @Test
  public void executeSqlRequestBuilderWithQueryOptions() {
    ExecuteSqlRequest request =
        context
            .getExecuteSqlRequestBuilder(
                Statement.newBuilder("SELECT FOO FROM BAR")
                    .withQueryOptions(QueryOptions.newBuilder().setOptimizerVersion("2.0").build())
                    .build(),
                QueryMode.NORMAL,
                Options.fromQueryOptions())
            .build();
    assertThat(request.getSql()).isEqualTo("SELECT FOO FROM BAR");
    assertThat(request.getQueryOptions().getOptimizerVersion()).isEqualTo("2.0");
  }
}
