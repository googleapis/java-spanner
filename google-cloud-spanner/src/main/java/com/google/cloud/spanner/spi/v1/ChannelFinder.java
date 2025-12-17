package com.google.cloud.spanner.spi.v1;

import com.google.spanner.v1.CacheUpdate;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.RoutingHint;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;

/**
 * ChannelFinder is responsible for finding the correct Spanner server to route RPCs to.
 *
 * <p>It uses a {@link KeyRecipeCache} and a {@link KeyRangeCache} to store metadata about the
 * database, including key recipes and range information. This metadata is updated through the
 * {@link #update(CacheUpdate)} method.
 *
 * <p>The {@link #findServer(ReadRequest.Builder)} method is used to determine the appropriate
 * server for a given read request.
 */
public final class ChannelFinder {
  private final String deployment;
  private final String databaseUri;
  private final KeyRangeCache rangeCache;
  private final KeyRecipeCache recipeCache;
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private long databaseId = 0L;
  private final ChannelFinderServerFactory serverFactory;

  public ChannelFinder(
      ChannelFinderServerFactory serverFactory, String deployment, String databaseUri) {
    this.serverFactory = serverFactory;
    this.deployment = deployment;
    this.databaseUri = databaseUri;
    this.rangeCache = new KeyRangeCache(serverFactory);
    this.recipeCache = new KeyRecipeCache();
  }

  /**
   * Updates the cache with new metadata.
   *
   * @param cacheUpdate The cache update information.
   */
  public void update(CacheUpdate cacheUpdate) {
    lock.writeLock().lock();
    try {
      if (databaseId != cacheUpdate.getDatabaseId()) {
        recipeCache.clear();
        rangeCache.clear();
        databaseId = cacheUpdate.getDatabaseId();
      }
      recipeCache.addRecipes(cacheUpdate.getKeyRecipes());
      rangeCache.addRanges(cacheUpdate);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Finds the server for a given ReadRequest.
   *
   * @param reqBuilder The ReadRequest builder.
   * @return The server to route the request to, or null if an error occurs.
   */
  @Nullable
  public ChannelFinderServer findServer(ReadRequest.Builder reqBuilder) {
    RoutingHint.Builder hintBuilder = reqBuilder.getRoutingHintBuilder();
    lock.readLock().lock();
    try {
      if (databaseId != 0) {
        hintBuilder.setDatabaseId(databaseId);
      }
      recipeCache.computeKeys(reqBuilder); // Modifies hintBuilder within reqBuilder
      return rangeCache.fillRoutingInfo(
          reqBuilder.getSession(), false, hintBuilder); // hintBuilder is already part of reqBuilder
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns a debug string representation of the cache.
   *
   * @return A string containing debug information.
   */
  public String debugString() {
    lock.readLock().lock();
    try {
      return rangeCache.debugString();
    } finally {
      lock.readLock().unlock();
    }
  }
}
