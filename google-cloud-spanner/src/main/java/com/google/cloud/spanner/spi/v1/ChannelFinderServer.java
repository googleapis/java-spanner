package com.google.cloud.spanner.spi.v1;

import io.grpc.ManagedChannel;

public interface ChannelFinderServer {
  String getAddress();

  boolean isHealthy();

  ManagedChannel getChannel(); // Added to get the underlying channel for RPC calls
}
