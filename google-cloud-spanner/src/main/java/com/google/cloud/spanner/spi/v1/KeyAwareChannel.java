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
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.protobuf.ByteString;
import com.google.spanner.v1.BeginTransactionRequest;
import com.google.spanner.v1.CacheUpdate;
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.Group;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.Range;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.ResultSet;
import com.google.spanner.v1.RollbackRequest;
import com.google.spanner.v1.RoutingHint;
import com.google.spanner.v1.Tablet;
import com.google.spanner.v1.Transaction;
import com.google.spanner.v1.TransactionSelector;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * ManagedChannel that routes eligible requests using location-aware routing hints.
 *
 * <p>Routing hints are applied to streaming read/query and unary ExecuteSql. Commit/Rollback use
 * transaction affinity when available. BeginTransaction is routed only when a mutation key is
 * provided.
 */
@InternalApi
final class KeyAwareChannel extends ManagedChannel {
  private static final Logger LOGGER = Logger.getLogger(KeyAwareChannel.class.getName());
  private static final String STREAMING_READ_METHOD = "google.spanner.v1.Spanner/StreamingRead";
  private static final String STREAMING_SQL_METHOD =
      "google.spanner.v1.Spanner/ExecuteStreamingSql";
  private static final String UNARY_SQL_METHOD = "google.spanner.v1.Spanner/ExecuteSql";
  private static final String BEGIN_TRANSACTION_METHOD =
      "google.spanner.v1.Spanner/BeginTransaction";
  private static final String COMMIT_METHOD = "google.spanner.v1.Spanner/Commit";
  private static final String ROLLBACK_METHOD = "google.spanner.v1.Spanner/Rollback";
  private static final String BYPASS_DEBUG_ENV = "SPANNER_BYPASS_DEBUG";
  private static final String BYPASS_DEBUG_VERBOSE_ENV = "SPANNER_BYPASS_DEBUG_VERBOSE";

  private static boolean isBypassDebugEnabled() {
    return Boolean.parseBoolean(System.getenv(BYPASS_DEBUG_ENV));
  }

  private static boolean isBypassDebugVerboseEnabled() {
    return Boolean.parseBoolean(System.getenv(BYPASS_DEBUG_VERBOSE_ENV));
  }

  private final ManagedChannel defaultChannel;
  private final ChannelEndpointCache endpointCache;
  private final String authority;
  private final String defaultEndpointAddress;
  private final Map<String, SoftReference<ChannelFinder>> channelFinders =
      new ConcurrentHashMap<>();
  private final Map<ByteString, String> transactionAffinities = new ConcurrentHashMap<>();

  private KeyAwareChannel(
      InstantiatingGrpcChannelProvider channelProvider,
      @Nullable ChannelEndpointCacheFactory endpointCacheFactory)
      throws IOException {
    if (endpointCacheFactory == null) {
      this.endpointCache = new GrpcChannelEndpointCache(channelProvider);
    } else {
      this.endpointCache = endpointCacheFactory.create(channelProvider);
    }
    this.defaultChannel = endpointCache.defaultChannel().getChannel();
    this.defaultEndpointAddress = endpointCache.defaultChannel().getAddress();
    this.authority = this.defaultChannel.authority();
  }

  static KeyAwareChannel create(
      InstantiatingGrpcChannelProvider channelProvider,
      @Nullable ChannelEndpointCacheFactory endpointCacheFactory)
      throws IOException {
    return new KeyAwareChannel(channelProvider, endpointCacheFactory);
  }

  private String extractDatabaseIdFromSession(String session) {
    if (session == null || session.isEmpty()) {
      return null;
    }
    int sessionsIndex = session.indexOf("/sessions/");
    if (sessionsIndex == -1) {
      return null;
    }
    return session.substring(0, sessionsIndex);
  }

  private ChannelFinder getOrCreateChannelFinder(String databaseId) {
    SoftReference<ChannelFinder> ref = channelFinders.get(databaseId);
    ChannelFinder finder = (ref != null) ? ref.get() : null;
    if (finder == null) {
      synchronized (channelFinders) {
        ref = channelFinders.get(databaseId);
        finder = (ref != null) ? ref.get() : null;
        if (finder == null) {
          finder = new ChannelFinder(endpointCache);
          channelFinders.put(databaseId, new SoftReference<>(finder));
        }
      }
    }
    return finder;
  }

  @Override
  public ManagedChannel shutdown() {
    endpointCache.shutdown();
    return this;
  }

  @Override
  public ManagedChannel shutdownNow() {
    endpointCache.shutdown();
    return this;
  }

  @Override
  public boolean isTerminated() {
    return defaultChannel.isTerminated();
  }

  @Override
  public boolean isShutdown() {
    return defaultChannel.isShutdown();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return defaultChannel.awaitTermination(timeout, unit);
  }

  @Override
  public String authority() {
    return authority;
  }

  @Override
  public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(
      MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
    if (isKeyAware(methodDescriptor)) {
      return new KeyAwareClientCall<>(this, methodDescriptor, callOptions);
    }
    return defaultChannel.newCall(methodDescriptor, callOptions);
  }

  private static boolean isKeyAware(MethodDescriptor<?, ?> methodDescriptor) {
    String method = methodDescriptor.getFullMethodName();
    return STREAMING_READ_METHOD.equals(method)
        || STREAMING_SQL_METHOD.equals(method)
        || UNARY_SQL_METHOD.equals(method)
        || BEGIN_TRANSACTION_METHOD.equals(method)
        || COMMIT_METHOD.equals(method)
        || ROLLBACK_METHOD.equals(method);
  }

  @Nullable
  private ChannelEndpoint affinityEndpoint(ByteString transactionId) {
    if (transactionId == null || transactionId.isEmpty()) {
      return null;
    }
    String address = transactionAffinities.get(transactionId);
    if (address == null) {
      return null;
    }
    return endpointCache.get(address);
  }

  private void clearAffinity(ByteString transactionId) {
    if (transactionId == null || transactionId.isEmpty()) {
      return;
    }
    transactionAffinities.remove(transactionId);
  }

  void clearTransactionAffinity(ByteString transactionId) {
    clearAffinity(transactionId);
  }

  private void recordAffinity(
      ByteString transactionId, @Nullable ChannelEndpoint endpoint, boolean allowDefault) {
    if (transactionId == null || transactionId.isEmpty() || endpoint == null) {
      return;
    }
    String address = endpoint.getAddress();
    if (!allowDefault && defaultEndpointAddress.equals(address)) {
      return;
    }
    transactionAffinities.put(transactionId, address);
  }

  private static ByteString transactionIdFromSelector(TransactionSelector selector) {
    if (selector.getSelectorCase() == TransactionSelector.SelectorCase.ID) {
      return selector.getId();
    }
    return ByteString.EMPTY;
  }

  @Nullable
  private static ByteString transactionIdFromMetadata(PartialResultSet result) {
    if (result.hasMetadata()) {
      return transactionIdFromTransaction(result.getMetadata().getTransaction());
    }
    return null;
  }

  @Nullable
  private static ByteString transactionIdFromMetadata(ResultSet result) {
    if (result.hasMetadata()) {
      return transactionIdFromTransaction(result.getMetadata().getTransaction());
    }
    return null;
  }

  @Nullable
  private static ByteString transactionIdFromTransaction(Transaction transaction) {
    if (transaction != null && !transaction.getId().isEmpty()) {
      return transaction.getId();
    }
    return null;
  }

  static final class KeyAwareClientCall<RequestT, ResponseT>
      extends ForwardingClientCall<RequestT, ResponseT> {
    private final KeyAwareChannel parentChannel;
    private final MethodDescriptor<RequestT, ResponseT> methodDescriptor;
    private final CallOptions callOptions;
    private Listener<ResponseT> responseListener;
    private Metadata headers;
    @Nullable private ClientCall<RequestT, ResponseT> delegate;
    private ChannelFinder channelFinder;
    @Nullable private ChannelEndpoint selectedEndpoint;
    @Nullable private ByteString transactionIdToClear;
    private boolean allowDefaultAffinity;
    @Nullable private String routingReason;
    @Nullable private ByteString routingTransactionId;
    private boolean routingHadMutationKey;
    private long pendingRequests;
    private boolean pendingHalfClose;
    @Nullable private Boolean pendingMessageCompression;
    private boolean cancelled;
    @Nullable private String cancelMessage;
    @Nullable private Throwable cancelCause;

    KeyAwareClientCall(
        KeyAwareChannel parentChannel,
        MethodDescriptor<RequestT, ResponseT> methodDescriptor,
        CallOptions callOptions) {
      this.parentChannel = parentChannel;
      this.methodDescriptor = methodDescriptor;
      this.callOptions = callOptions;
    }

    @Override
    protected ClientCall<RequestT, ResponseT> delegate() {
      if (delegate == null) {
        throw new IllegalStateException(
            "Delegate call not initialized before use. sendMessage was likely not called.");
      }
      return delegate;
    }

    @Override
    public void start(Listener<ResponseT> responseListener, Metadata headers) {
      this.responseListener = new KeyAwareClientCallListener<>(responseListener, this);
      this.headers = headers;
      if (cancelled) {
        this.responseListener.onClose(
            io.grpc.Status.CANCELLED.withDescription(cancelMessage).withCause(cancelCause),
            new Metadata());
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sendMessage(RequestT message) {
      if (cancelled) {
        return;
      }
      if (responseListener == null || headers == null) {
        throw new IllegalStateException("start must be called before sendMessage");
      }
      routingReason = null;
      routingTransactionId = null;
      routingHadMutationKey = false;
      ChannelEndpoint endpoint = null;
      ChannelFinder finder = null;

      if (message instanceof ReadRequest) {
        ReadRequest.Builder reqBuilder = ((ReadRequest) message).toBuilder();
        RoutingDecision routing = routeFromRequest(reqBuilder);
        finder = routing.finder;
        endpoint = routing.endpoint;
        message = (RequestT) reqBuilder.build();
      } else if (message instanceof ExecuteSqlRequest) {
        ExecuteSqlRequest.Builder reqBuilder = ((ExecuteSqlRequest) message).toBuilder();
        RoutingDecision routing = routeFromRequest(reqBuilder);
        finder = routing.finder;
        endpoint = routing.endpoint;
        message = (RequestT) reqBuilder.build();
      } else if (message instanceof BeginTransactionRequest) {
        BeginTransactionRequest.Builder reqBuilder =
            ((BeginTransactionRequest) message).toBuilder();
        String databaseId = parentChannel.extractDatabaseIdFromSession(reqBuilder.getSession());
        if (databaseId != null && reqBuilder.hasMutationKey()) {
          routingHadMutationKey = true;
          finder = parentChannel.getOrCreateChannelFinder(databaseId);
          ChannelEndpoint routed = finder.findServer(reqBuilder);
          if (endpoint == null) {
            endpoint = routed;
          }
        }
        allowDefaultAffinity = true;
        message = (RequestT) reqBuilder.build();
      } else if (message instanceof CommitRequest) {
        CommitRequest request = (CommitRequest) message;
        if (!request.getTransactionId().isEmpty()) {
          routingTransactionId = request.getTransactionId();
          endpoint = parentChannel.affinityEndpoint(request.getTransactionId());
          transactionIdToClear = request.getTransactionId();
        }
      } else if (message instanceof RollbackRequest) {
        RollbackRequest request = (RollbackRequest) message;
        if (!request.getTransactionId().isEmpty()) {
          routingTransactionId = request.getTransactionId();
          endpoint = parentChannel.affinityEndpoint(request.getTransactionId());
          transactionIdToClear = request.getTransactionId();
        }
      } else {
        throw new IllegalStateException(
            "Only read, query, begin transaction, commit, and rollback requests are supported for"
                + " key-aware calls.");
      }

      boolean usedDefault = false;
      if (endpoint == null) {
        usedDefault = true;
        endpoint = parentChannel.endpointCache.defaultChannel();
      }
      selectedEndpoint = endpoint;
      this.channelFinder = finder;

      logRoutingDecision(message, endpoint, usedDefault);

      delegate = endpoint.getChannel().newCall(methodDescriptor, callOptions);
      if (pendingMessageCompression != null) {
        delegate.setMessageCompression(pendingMessageCompression);
      }
      delegate.start(responseListener, headers);
      drainPendingRequests();
      delegate.sendMessage(message);
      if (pendingHalfClose) {
        delegate.halfClose();
      }
    }

    @Override
    public void halfClose() {
      if (delegate != null) {
        delegate.halfClose();
      } else {
        pendingHalfClose = true;
      }
    }

    @Override
    public void cancel(@Nullable String message, @Nullable Throwable cause) {
      if (delegate != null) {
        delegate.cancel(message, cause);
      } else if (responseListener != null) {
        responseListener.onClose(
            io.grpc.Status.CANCELLED.withDescription(message).withCause(cause), new Metadata());
        cancelled = true;
        cancelMessage = message;
        cancelCause = cause;
      } else {
        cancelled = true;
        cancelMessage = message;
        cancelCause = cause;
      }
    }

    @Override
    public void request(int numMessages) {
      if (delegate != null) {
        delegate.request(numMessages);
        return;
      }
      if (numMessages <= 0) {
        return;
      }
      long updated = pendingRequests + numMessages;
      if (updated < 0L) {
        updated = Long.MAX_VALUE;
      }
      pendingRequests = updated;
    }

    @Override
    public boolean isReady() {
      if (delegate == null) {
        return false;
      }
      return delegate.isReady();
    }

    @Override
    public void setMessageCompression(boolean enabled) {
      if (delegate != null) {
        delegate.setMessageCompression(enabled);
      } else {
        pendingMessageCompression = enabled;
      }
    }

    private void drainPendingRequests() {
      long requests = pendingRequests;
      pendingRequests = 0L;
      while (requests > 0) {
        int batch = requests > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) requests;
        delegate.request(batch);
        requests -= batch;
      }
    }

    void maybeRecordAffinity(ByteString transactionId) {
      parentChannel.recordAffinity(transactionId, selectedEndpoint, allowDefaultAffinity);
    }

    void maybeClearAffinity() {
      parentChannel.clearAffinity(transactionIdToClear);
    }

    private RoutingDecision routeFromRequest(ReadRequest.Builder reqBuilder) {
      String databaseId = parentChannel.extractDatabaseIdFromSession(reqBuilder.getSession());
      ByteString transactionId = transactionIdFromSelector(reqBuilder.getTransaction());
      routingTransactionId = transactionId;
      ChannelEndpoint endpoint = parentChannel.affinityEndpoint(transactionId);
      ChannelFinder finder = null;
      if (databaseId != null) {
        finder = parentChannel.getOrCreateChannelFinder(databaseId);
        ChannelEndpoint routed = finder.findServer(reqBuilder);
        if (endpoint == null) {
          endpoint = routed;
        }
      }
      return new RoutingDecision(finder, endpoint);
    }

    private RoutingDecision routeFromRequest(ExecuteSqlRequest.Builder reqBuilder) {
      String databaseId = parentChannel.extractDatabaseIdFromSession(reqBuilder.getSession());
      ByteString transactionId = transactionIdFromSelector(reqBuilder.getTransaction());
      routingTransactionId = transactionId;
      ChannelEndpoint endpoint = parentChannel.affinityEndpoint(transactionId);
      ChannelFinder finder = null;
      if (databaseId != null) {
        finder = parentChannel.getOrCreateChannelFinder(databaseId);
        ChannelEndpoint routed = finder.findServer(reqBuilder);
        if (endpoint == null) {
          endpoint = routed;
        }
      }
      return new RoutingDecision(finder, endpoint);
    }

    private void logRoutingDecision(
        RequestT message, ChannelEndpoint endpoint, boolean usedDefault) {
      if (!isBypassDebugEnabled() || endpoint == null) {
        return;
      }
      String endpointAddress = endpoint.getAddress();
      boolean isBypass = !parentChannel.defaultEndpointAddress.equals(endpointAddress);
      String method = methodDescriptor.getFullMethodName();
      String session = extractSession(message);
      String details = buildDetails(message);
      String reason = computeRoutingReason(message, isBypass, usedDefault);
      LOGGER.log(
          Level.INFO,
          "SpanFE bypass routing: method={0}, bypass={1}, endpoint={2}, session={3}, reason={4}{5}",
          new Object[] {method, isBypass, endpointAddress, session, reason, details});
    }

    private static String extractSession(Object message) {
      if (message instanceof ReadRequest) {
        return ((ReadRequest) message).getSession();
      }
      if (message instanceof ExecuteSqlRequest) {
        return ((ExecuteSqlRequest) message).getSession();
      }
      if (message instanceof BeginTransactionRequest) {
        return ((BeginTransactionRequest) message).getSession();
      }
      if (message instanceof CommitRequest) {
        return ((CommitRequest) message).getSession();
      }
      if (message instanceof RollbackRequest) {
        return ((RollbackRequest) message).getSession();
      }
      return "";
    }

    private String buildDetails(Object message) {
      if (message instanceof ReadRequest) {
        ReadRequest request = (ReadRequest) message;
        return String.format(
            ", op=read, table=%s, index=%s%s",
            request.getTable(), request.getIndex(), routingHintSummary(request.getRoutingHint()));
      }
      if (message instanceof ExecuteSqlRequest) {
        ExecuteSqlRequest request = (ExecuteSqlRequest) message;
        return String.format(
            ", op=executeSql, hasSeqno=%s%s",
            request.getSeqno() != 0L, routingHintSummary(request.getRoutingHint()));
      }
      if (message instanceof BeginTransactionRequest) {
        BeginTransactionRequest request = (BeginTransactionRequest) message;
        return String.format(", op=beginTransaction, hasMutationKey=%s", request.hasMutationKey());
      }
      if (message instanceof CommitRequest) {
        CommitRequest request = (CommitRequest) message;
        return String.format(", op=commit, txnIdPresent=%s", !request.getTransactionId().isEmpty());
      }
      if (message instanceof RollbackRequest) {
        RollbackRequest request = (RollbackRequest) message;
        return String.format(
            ", op=rollback, txnIdPresent=%s", !request.getTransactionId().isEmpty());
      }
      return "";
    }

    private String computeRoutingReason(Object message, boolean isBypass, boolean usedDefault) {
      if (!usedDefault && routingReason != null) {
        return routingReason;
      }
      if (message instanceof BeginTransactionRequest) {
        if (!routingHadMutationKey) {
          return "default_no_mutation_key";
        }
        return isBypass ? "bypass_routing_hint" : "default_no_cache";
      }
      if (message instanceof CommitRequest || message instanceof RollbackRequest) {
        if (routingTransactionId == null || routingTransactionId.isEmpty()) {
          return "default_missing_transaction_id";
        }
        if (isBypass) {
          return "bypass_affinity_hit";
        }
        return "default_affinity_miss";
      }
      if (message instanceof ReadRequest || message instanceof ExecuteSqlRequest) {
        if (routingTransactionId != null && !routingTransactionId.isEmpty()) {
          return isBypass ? "bypass_affinity_hit" : "default_affinity_miss";
        }
        return isBypass ? "bypass_routing_hint" : "default_no_cache";
      }
      return isBypass ? "bypass_routing_hint" : "default_no_cache";
    }

    private static String routingHintSummary(RoutingHint hint) {
      if (hint == null) {
        return "";
      }
      if (hint.getDatabaseId() == 0
          && hint.getGroupUid() == 0
          && hint.getSplitId() == 0
          && hint.getTabletUid() == 0) {
        return "";
      }
      String reason = "";
      if (isBypassDebugVerboseEnabled()) {
        if (hint.getDatabaseId() == 0) {
          reason = ", hintReason=missing_database_id";
        } else if (hint.getGroupUid() == 0 || hint.getSplitId() == 0 || hint.getTabletUid() == 0) {
          reason = ", hintReason=missing_target";
        } else {
          reason = ", hintReason=complete";
        }
      }
      StringBuilder summary = new StringBuilder();
      summary.append(
          String.format(
              ", routingHint={db=%s,group=%s,split=%s,tablet=%s,opUid=%s%s",
              hint.getDatabaseId(),
              hint.getGroupUid(),
              hint.getSplitId(),
              hint.getTabletUid(),
              hint.getOperationUid(),
              reason));
      if (isBypassDebugVerboseEnabled()) {
        summary.append(", schemaGenBytes=").append(hint.getSchemaGeneration().size());
        summary.append(", key=").append(formatBytes(hint.getKey()));
        summary.append(", limitKey=").append(formatBytes(hint.getLimitKey()));
      }
      summary.append("}");
      return summary.toString();
    }
  }

  private static final class RoutingDecision {
    @Nullable private final ChannelFinder finder;
    @Nullable private final ChannelEndpoint endpoint;

    private RoutingDecision(@Nullable ChannelFinder finder, @Nullable ChannelEndpoint endpoint) {
      this.finder = finder;
      this.endpoint = endpoint;
    }
  }

  static final class KeyAwareClientCallListener<ResponseT>
      extends SimpleForwardingClientCallListener<ResponseT> {
    private final KeyAwareClientCall<?, ResponseT> call;

    KeyAwareClientCallListener(
        ClientCall.Listener<ResponseT> responseListener, KeyAwareClientCall<?, ResponseT> call) {
      super(responseListener);
      this.call = call;
    }

    @Override
    public void onMessage(ResponseT message) {
      ByteString transactionId = null;
      if (message instanceof PartialResultSet) {
        PartialResultSet response = (PartialResultSet) message;
        if (response.hasCacheUpdate() && call.channelFinder != null) {
          call.channelFinder.update(response.getCacheUpdate());
          logCacheUpdate(response.getCacheUpdate());
        }
        transactionId = transactionIdFromMetadata(response);
      } else if (message instanceof ResultSet) {
        ResultSet response = (ResultSet) message;
        transactionId = transactionIdFromMetadata(response);
      } else if (message instanceof Transaction) {
        Transaction response = (Transaction) message;
        transactionId = transactionIdFromTransaction(response);
      }
      if (transactionId != null) {
        call.maybeRecordAffinity(transactionId);
      }
      super.onMessage(message);
    }

    @Override
    public void onClose(io.grpc.Status status, Metadata trailers) {
      call.maybeClearAffinity();
      super.onClose(status, trailers);
    }
  }

  private static void logCacheUpdate(CacheUpdate update) {
    if (!isBypassDebugVerboseEnabled()) {
      return;
    }
    StringBuilder sb = new StringBuilder(256);
    int recipes = update.hasKeyRecipes() ? update.getKeyRecipes().getRecipeCount() : 0;
    int schemaGenBytes =
        update.hasKeyRecipes() ? update.getKeyRecipes().getSchemaGeneration().size() : 0;
    sb.append("SpanFE bypass cache update: db=")
        .append(update.getDatabaseId())
        .append(", recipes=")
        .append(recipes)
        .append(", schemaGenBytes=")
        .append(schemaGenBytes)
        .append(", groups=")
        .append(update.getGroupCount())
        .append(", ranges=")
        .append(update.getRangeCount());

    for (Group group : update.getGroupList()) {
      sb.append("\n  Group uid=")
          .append(group.getGroupUid())
          .append(" leaderIndex=")
          .append(group.getLeaderIndex())
          .append(" tablets=");
      if (group.getTabletsCount() == 0) {
        sb.append("[]");
      } else {
        sb.append("[");
        for (int i = 0; i < group.getTabletsCount(); i++) {
          Tablet tablet = group.getTablets(i);
          if (i > 0) {
            sb.append(", ");
          }
          sb.append(tablet.getTabletUid())
              .append("@")
              .append(tablet.getServerAddress())
              .append("(role=")
              .append(tablet.getRole().name())
              .append(",distance=")
              .append(tablet.getDistance())
              .append(",skip=")
              .append(tablet.getSkip())
              .append(")");
        }
        sb.append("]");
      }
    }

    for (Range range : update.getRangeList()) {
      sb.append("\n  Range groupUid=")
          .append(range.getGroupUid())
          .append(" splitId=")
          .append(range.getSplitId())
          .append(" start=")
          .append(formatBytes(range.getStartKey()))
          .append(" limit=")
          .append(formatBytes(range.getLimitKey()));
    }

    LOGGER.log(Level.INFO, sb.toString());
  }

  private static String formatBytes(ByteString bytes) {
    if (bytes == null || bytes.isEmpty()) {
      return "";
    }
    int limit = Math.min(bytes.size(), 16);
    StringBuilder sb = new StringBuilder(limit * 2 + 3);
    sb.append("0x");
    for (int i = 0; i < limit; i++) {
      int v = bytes.byteAt(i) & 0xff;
      sb.append(Character.forDigit((v >>> 4) & 0xf, 16));
      sb.append(Character.forDigit(v & 0xf, 16));
    }
    if (bytes.size() > limit) {
      sb.append("…");
    }
    return sb.toString();
  }
}
