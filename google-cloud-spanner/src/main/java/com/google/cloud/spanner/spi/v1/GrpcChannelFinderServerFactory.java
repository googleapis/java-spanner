package com.google.cloud.spanner.spi.v1;

import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import io.grpc.ManagedChannel;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Corresponds to ServerFactory and Server in C++
class GrpcChannelFinderServerFactory implements ChannelFinderServerFactory {
  private final InstantiatingGrpcChannelProvider.Builder channelBuilder;
  private final Map<String, GrpcChannelFinderServer> servers = new ConcurrentHashMap<>();
  private final GrpcChannelFinderServer defaultServer;

  public GrpcChannelFinderServerFactory(InstantiatingGrpcChannelProvider.Builder channelBuilder)
      throws IOException {
    this.channelBuilder = channelBuilder;
    // The "default" server will use the original endpoint from the builder.
    this.defaultServer =
        new GrpcChannelFinderServer(this.channelBuilder.getEndpoint(), channelBuilder.build());
    this.servers.put(this.defaultServer.getAddress(), this.defaultServer);
  }

  @Override
  public ChannelFinderServer defaultServer() {
    return defaultServer;
  }

  @Override
  public ChannelFinderServer create(String address) {
    return servers.computeIfAbsent(
        address,
        addr -> {
          try {
            // Modify the builder to use the new address
            synchronized (channelBuilder) {
              InstantiatingGrpcChannelProvider.Builder newBuilder =
                  channelBuilder.setEndpoint(addr);
              return new GrpcChannelFinderServer(addr, newBuilder.build());
            }
          } catch (IOException e) {
            throw new RuntimeException("Failed to create channel for address: " + addr, e);
          }
        });
  }

  static class GrpcChannelFinderServer implements ChannelFinderServer {
    private final String address;
    private final ManagedChannel channel;

    public GrpcChannelFinderServer(String address, InstantiatingGrpcChannelProvider provider)
        throws IOException {
      this.address = address;
      // It's assumed that getTransportChannel() returns a ManagedChannel or can be cast to one.
      // For this example, GrpcTransportChannel is used as in KeyAwareChannel.
      GrpcTransportChannel transportChannel = (GrpcTransportChannel) provider.getTransportChannel();
      this.channel = (ManagedChannel) transportChannel.getChannel();
    }

    // Constructor for the default server that already has a channel
    public GrpcChannelFinderServer(String address, ManagedChannel channel) {
      this.address = address;
      this.channel = channel;
    }

    @Override
    public String getAddress() {
      return address;
    }

    @Override
    public boolean isHealthy() {
      // A simple health check. In a real scenario, this might involve a ping or other checks.
      return !channel.isShutdown() && !channel.isTerminated();
    }

    @Override
    public ManagedChannel getChannel() {
      return channel;
    }
  }
}
