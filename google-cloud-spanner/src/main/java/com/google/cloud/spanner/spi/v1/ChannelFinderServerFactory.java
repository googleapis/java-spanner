package com.google.cloud.spanner.spi.v1;

public interface ChannelFinderServerFactory {
  ChannelFinderServer defaultServer();

  ChannelFinderServer create(String address);
}
