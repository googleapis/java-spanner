package com.google.cloud.spanner;

import com.google.api.core.InternalApi;

/**
 * Simple helper class to get access to a package-private method in the {@link
 * com.google.cloud.spanner.SessionPoolOptions}.
 */
@InternalApi
public class SessionPoolOptionsHelper {

  // TODO: Remove when Builder.setUseMultiplexedSession(..) has been made public.
  public static SessionPoolOptions.Builder setUseMultiplexedSession(
      SessionPoolOptions.Builder sessionPoolOptionsBuilder, boolean useMultiplexedSession) {
    return sessionPoolOptionsBuilder.setUseMultiplexedSession(useMultiplexedSession);
  }
}
