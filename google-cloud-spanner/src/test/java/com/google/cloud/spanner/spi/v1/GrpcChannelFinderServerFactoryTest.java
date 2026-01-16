/*
 * Copyright 2026 Google LLC
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

package com.google.cloud.spanner.spi.v1;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.cloud.spanner.SpannerException;
import io.grpc.ManagedChannelBuilder;
import org.junit.Test;

public class GrpcChannelFinderServerFactoryTest {

  private static InstantiatingGrpcChannelProvider createProvider(String endpoint) {
    return InstantiatingGrpcChannelProvider.newBuilder()
        .setEndpoint(endpoint)
        .setChannelConfigurator(ManagedChannelBuilder::usePlaintext)
        .build();
  }

  @Test
  public void defaultServerIsCached() throws Exception {
    GrpcChannelFinderServerFactory factory =
        new GrpcChannelFinderServerFactory(createProvider("localhost:1234"));
    try {
      ChannelFinderServer defaultServer = factory.defaultServer();
      ChannelFinderServer server = factory.create(defaultServer.getAddress());
      assertThat(server).isSameInstanceAs(defaultServer);
    } finally {
      factory.shutdown();
    }
  }

  @Test
  public void createCachesPerAddress() throws Exception {
    GrpcChannelFinderServerFactory factory =
        new GrpcChannelFinderServerFactory(createProvider("localhost:1234"));
    try {
      ChannelFinderServer first = factory.create("localhost:1111");
      ChannelFinderServer second = factory.create("localhost:1111");
      ChannelFinderServer third = factory.create("localhost:2222");

      assertThat(second).isSameInstanceAs(first);
      assertThat(third).isNotSameInstanceAs(first);
    } finally {
      factory.shutdown();
    }
  }

  @Test
  public void evictRemovesNonDefaultServer() throws Exception {
    GrpcChannelFinderServerFactory factory =
        new GrpcChannelFinderServerFactory(createProvider("localhost:1234"));
    try {
      ChannelFinderServer first = factory.create("localhost:1111");
      factory.evict("localhost:1111");
      ChannelFinderServer second = factory.create("localhost:1111");

      assertThat(second).isNotSameInstanceAs(first);
    } finally {
      factory.shutdown();
    }
  }

  @Test
  public void evictIgnoresDefaultServer() throws Exception {
    GrpcChannelFinderServerFactory factory =
        new GrpcChannelFinderServerFactory(createProvider("localhost:1234"));
    try {
      ChannelFinderServer defaultServer = factory.defaultServer();
      factory.evict(defaultServer.getAddress());
      ChannelFinderServer server = factory.create(defaultServer.getAddress());

      assertThat(server).isSameInstanceAs(defaultServer);
    } finally {
      factory.shutdown();
    }
  }

  @Test
  public void shutdownPreventsNewServers() throws Exception {
    GrpcChannelFinderServerFactory factory =
        new GrpcChannelFinderServerFactory(createProvider("localhost:1234"));
    factory.shutdown();

    assertThrows(SpannerException.class, () -> factory.create("localhost:1111"));
    assertThat(factory.defaultServer().getChannel().isShutdown()).isTrue();
  }

  @Test
  public void healthReflectsChannelShutdown() throws Exception {
    GrpcChannelFinderServerFactory factory =
        new GrpcChannelFinderServerFactory(createProvider("localhost:1234"));
    try {
      ChannelFinderServer server = factory.create("localhost:1111");
      assertThat(server.isHealthy()).isTrue();

      server.getChannel().shutdownNow();
      assertThat(server.isHealthy()).isFalse();
    } finally {
      factory.shutdown();
    }
  }
}
