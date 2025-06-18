package com.google.cloud.spanner;

class TestHelper {

  static boolean isMultiplexSessionDisabled() {
    return System.getenv()
        .getOrDefault("GOOGLE_CLOUD_SPANNER_MULTIPLEXED_SESSIONS", "")
        .equalsIgnoreCase("false");
  }
}
