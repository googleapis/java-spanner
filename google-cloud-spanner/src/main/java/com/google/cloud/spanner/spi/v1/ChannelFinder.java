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
        System.out.println("DEBUG [BYPASS]: Database ID changed from " + databaseId 
            + " to " + cacheUpdate.getDatabaseId() + ", clearing caches");
        recipeCache.clear();
        rangeCache.clear();
        databaseId = cacheUpdate.getDatabaseId();
      }
      recipeCache.addRecipes(cacheUpdate.getKeyRecipes());
      rangeCache.addRanges(cacheUpdate);
      System.out.println("DEBUG [BYPASS]: Cache updated. Current state:\n" + rangeCache.debugString());
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
      System.out.println("DEBUG [BYPASS]: findServer - computing keys for table: " 
          + reqBuilder.getTable());
      recipeCache.computeKeys(reqBuilder); // Modifies hintBuilder within reqBuilder
      System.out.println("DEBUG [BYPASS]: findServer - after computeKeys, key: " 
          + hintBuilder.getKey().toStringUtf8());
      ChannelFinderServer server = rangeCache.fillRoutingInfo(
          reqBuilder.getSession(), false, hintBuilder);
      System.out.println("DEBUG [BYPASS]: findServer - fillRoutingInfo returned server: " 
          + (server != null ? server.getAddress() : "null"));
      return server;
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
