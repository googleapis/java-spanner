/*
 * Copyright 2017 Google LLC
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

import com.google.api.gax.paging.Page;
import com.google.cloud.BaseService;
import com.google.cloud.PageImpl;
import com.google.cloud.PageImpl.NextPageFetcher;
import com.google.cloud.grpc.GrpcTransportOptions;
import com.google.cloud.spanner.SessionClient.SessionId;
import com.google.cloud.spanner.spi.v1.SpannerRpc;
import com.google.cloud.spanner.spi.v1.SpannerRpc.Paginated;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/** Default implementation of the Cloud Spanner interface. */
class SpannerImpl extends BaseService<SpannerOptions> implements Spanner {
  private static final Logger logger = Logger.getLogger(SpannerImpl.class.getName());
  static final Tracer tracer = Tracing.getTracer();

  static final String CREATE_SESSION = "CloudSpannerOperation.CreateSession";
  static final String BATCH_CREATE_SESSIONS = "CloudSpannerOperation.BatchCreateSessions";
  static final String BATCH_CREATE_SESSIONS_REQUEST =
      "CloudSpannerOperation.BatchCreateSessionsRequest";
  static final String DELETE_SESSION = "CloudSpannerOperation.DeleteSession";
  static final String BEGIN_TRANSACTION = "CloudSpannerOperation.BeginTransaction";
  static final String COMMIT = "CloudSpannerOperation.Commit";
  static final String QUERY = "CloudSpannerOperation.ExecuteStreamingQuery";
  static final String READ = "CloudSpannerOperation.ExecuteStreamingRead";

  private final SpannerRpc gapicRpc;

  @GuardedBy("this")
  private final Map<DatabaseId, DatabaseClientImpl> dbClients = new HashMap<>();

  @GuardedBy("this")
  private final List<DatabaseClientImpl> invalidatedDbClients = new ArrayList<>();

  @GuardedBy("this")
  private final Map<DatabaseId, SessionClient> sessionClients = new HashMap<>();

  private final DatabaseAdminClient dbAdminClient;
  private final InstanceAdminClient instanceClient;

  @GuardedBy("this")
  private boolean spannerIsClosed = false;

  @VisibleForTesting
  SpannerImpl(SpannerRpc gapicRpc, SpannerOptions options) {
    super(options);
    this.gapicRpc = gapicRpc;
    this.dbAdminClient = new DatabaseAdminClientImpl(options.getProjectId(), gapicRpc);
    this.instanceClient =
        new InstanceAdminClientImpl(options.getProjectId(), gapicRpc, dbAdminClient);
  }

  SpannerImpl(SpannerOptions options) {
    this(options.getSpannerRpcV1(), options);
  }

  /** Returns the {@link SpannerRpc} of this {@link SpannerImpl} instance. */
  SpannerRpc getRpc() {
    return gapicRpc;
  }

  /** Returns the default setting for prefetchChunks of this {@link SpannerImpl} instance. */
  int getDefaultPrefetchChunks() {
    return getOptions().getPrefetchChunks();
  }

  SessionImpl sessionWithId(String name) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "name is null or empty");
    SessionId id = SessionId.of(name);
    return getSessionClient(id.getDatabaseId()).sessionWithId(name);
  }

  SessionClient getSessionClient(DatabaseId db) {
    synchronized (this) {
      Preconditions.checkState(!spannerIsClosed, "Cloud Spanner client has been closed");
      if (sessionClients.containsKey(db)) {
        return sessionClients.get(db);
      } else {
        SessionClient client =
            new SessionClient(
                this,
                db,
                ((GrpcTransportOptions) getOptions().getTransportOptions()).getExecutorFactory());
        sessionClients.put(db, client);
        return client;
      }
    }
  }

  @Override
  public DatabaseAdminClient getDatabaseAdminClient() {
    return dbAdminClient;
  }

  @Override
  public InstanceAdminClient getInstanceAdminClient() {
    return instanceClient;
  }

  @Override
  public DatabaseClient getDatabaseClient(DatabaseId db) {
    synchronized (this) {
      Preconditions.checkState(!spannerIsClosed, "Cloud Spanner client has been closed");
      if (dbClients.containsKey(db) && !dbClients.get(db).pool.isValid()) {
        // Move the invalidated client to a separate list, so we can close it together with the
        // other database clients when the Spanner instance is closed.
        invalidatedDbClients.add(dbClients.get(db));
        dbClients.remove(db);
      }
      if (dbClients.containsKey(db)) {
        return dbClients.get(db);
      } else {
        SessionPool pool =
            SessionPool.createPool(getOptions(), SpannerImpl.this.getSessionClient(db));
        DatabaseClientImpl dbClient = createDatabaseClient(pool);
        dbClients.put(db, dbClient);
        return dbClient;
      }
    }
  }

  @VisibleForTesting
  DatabaseClientImpl createDatabaseClient(SessionPool pool) {
    return new DatabaseClientImpl(pool);
  }

  @Override
  public BatchClient getBatchClient(DatabaseId db) {
    return new BatchClientImpl(getSessionClient(db));
  }

  @Override
  public void close() {
    List<ListenableFuture<Void>> closureFutures = null;
    synchronized (this) {
      Preconditions.checkState(!spannerIsClosed, "Cloud Spanner client has been closed");
      spannerIsClosed = true;
      closureFutures = new ArrayList<>();
      invalidatedDbClients.addAll(dbClients.values());
      for (DatabaseClientImpl dbClient : invalidatedDbClients) {
        closureFutures.add(dbClient.closeAsync());
      }
      dbClients.clear();
    }
    try {
      Futures.successfulAsList(closureFutures).get();
    } catch (InterruptedException | ExecutionException e) {
      throw SpannerExceptionFactory.newSpannerException(e);
    }
    for (SessionClient sessionClient : sessionClients.values()) {
      sessionClient.close();
    }
    sessionClients.clear();
    try {
      gapicRpc.shutdown();
    } catch (RuntimeException e) {
      logger.log(Level.WARNING, "Failed to close channels", e);
    }
  }

  @Override
  public boolean isClosed() {
    return spannerIsClosed;
  }

  /** Helper class for gRPC calls that can return paginated results. */
  abstract static class PageFetcher<S, T> implements NextPageFetcher<S> {
    private String nextPageToken;

    @Override
    public Page<S> getNextPage() {
      Paginated<T> nextPage = getNextPage(nextPageToken);
      this.nextPageToken = nextPage.getNextPageToken();
      List<S> results = new ArrayList<>();
      for (T proto : nextPage.getResults()) {
        results.add(fromProto(proto));
      }
      return new PageImpl<S>(this, nextPageToken, results);
    }

    void setNextPageToken(String nextPageToken) {
      this.nextPageToken = nextPageToken;
    }

    abstract Paginated<T> getNextPage(@Nullable String nextPageToken);

    abstract S fromProto(T proto);
  }
}
