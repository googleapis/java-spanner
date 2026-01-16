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

import com.google.api.core.InternalApi;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.common.annotations.VisibleForTesting;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * gRPC implementation of {@link ChannelFinderServerFactory}.
 *
 * <p>This factory creates and caches gRPC channels per address. It uses {@link
 * InstantiatingGrpcChannelProvider#withEndpoint(String)} to create new channels with the same
 * configuration but different endpoints, avoiding race conditions.
 */
@InternalApi
class GrpcChannelFinderServerFactory implements ChannelFinderServerFactory {

  /** Timeout for graceful channel shutdown. */
  private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

  private final InstantiatingGrpcChannelProvider baseProvider;
  private final Map<String, GrpcChannelFinderServer> servers = new ConcurrentHashMap<>();
  private final GrpcChannelFinderServer defaultServer;
  private volatile boolean isShutdown = false;

  /**
   * Creates a new factory with the given channel provider.
   *
   * @param channelProvider the base provider used to create channels. New channels for different
   *     endpoints are created using {@link InstantiatingGrpcChannelProvider#withEndpoint(String)}.
   * @throws IOException if the default channel cannot be created
   */
  public GrpcChannelFinderServerFactory(InstantiatingGrpcChannelProvider channelProvider)
      throws IOException {
    this.baseProvider = channelProvider;
    String defaultEndpoint = channelProvider.getEndpoint();
    this.defaultServer = new GrpcChannelFinderServer(defaultEndpoint, channelProvider);
    this.servers.put(defaultEndpoint, this.defaultServer);
  }

  @Override
  public ChannelFinderServer defaultServer() {
    return defaultServer;
  }

  @Override
  public ChannelFinderServer create(String address) {
    if (isShutdown) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.FAILED_PRECONDITION, "ChannelFinderServerFactory has been shut down");
    }

    return servers.computeIfAbsent(
        address,
        addr -> {
          try {
            // Create a new provider with the same config but different endpoint.
            // This is thread-safe as withEndpoint() returns a new provider instance.
            TransportChannelProvider newProvider = baseProvider.withEndpoint(addr);
            return new GrpcChannelFinderServer(addr, newProvider);
          } catch (IOException e) {
            throw SpannerExceptionFactory.newSpannerException(
                ErrorCode.INTERNAL, "Failed to create channel for address: " + addr, e);
          }
        });
  }

  @Override
  public void evict(String address) {
    if (defaultServer.getAddress().equals(address)) {
      return;
    }
    GrpcChannelFinderServer server = servers.remove(address);
    if (server != null) {
      shutdownServerGracefully(server);
    }
  }

  @Override
  public void shutdown() {
    isShutdown = true;
    for (GrpcChannelFinderServer server : servers.values()) {
      shutdownServerGracefully(server);
    }
    servers.clear();
  }

  /**
   * Gracefully shuts down a server's channel.
   *
   * <p>First attempts a graceful shutdown, waiting for in-flight RPCs to complete. If the timeout
   * is exceeded, forces immediate shutdown.
   */
  private void shutdownServerGracefully(GrpcChannelFinderServer server) {
    ManagedChannel channel = server.getChannel();
    if (channel.isShutdown()) {
      return;
    }

    channel.shutdown();
    try {
      if (!channel.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        channel.shutdownNow();
      }
    } catch (InterruptedException e) {
      channel.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /** gRPC implementation of {@link ChannelFinderServer}. */
  static class GrpcChannelFinderServer implements ChannelFinderServer {
    private final String address;
    private final ManagedChannel channel;

    /**
     * Creates a server from a channel provider.
     *
     * @param address the server address
     * @param provider the channel provider (must be a gRPC provider)
     * @throws IOException if the channel cannot be created
     */
    GrpcChannelFinderServer(String address, TransportChannelProvider provider) throws IOException {
      this.address = address;
      TransportChannelProvider readyProvider = provider;
      if (provider.needsHeaders()) {
        readyProvider = provider.withHeaders(java.util.Collections.emptyMap());
      }
      GrpcTransportChannel transportChannel =
          (GrpcTransportChannel) readyProvider.getTransportChannel();
      this.channel = (ManagedChannel) transportChannel.getChannel();
    }

    /**
     * Creates a server with an existing channel. Primarily for testing.
     *
     * @param address the server address
     * @param channel the managed channel
     */
    @VisibleForTesting
    GrpcChannelFinderServer(String address, ManagedChannel channel) {
      this.address = address;
      this.channel = channel;
    }

    @Override
    public String getAddress() {
      return address;
    }

    @Override
    public boolean isHealthy() {
      if (channel.isShutdown() || channel.isTerminated()) {
        return false;
      }
      // Check connectivity state without triggering a connection attempt.
      // Some channel implementations don't support getState(), in which case
      // we assume the channel is healthy if it's not shutdown/terminated.
      try {
        ConnectivityState state = channel.getState(false);
        return state != ConnectivityState.SHUTDOWN && state != ConnectivityState.TRANSIENT_FAILURE;
      } catch (UnsupportedOperationException e) {
        return true;
      }
    }

    @Override
    public ManagedChannel getChannel() {
      return channel;
    }
  }
}
