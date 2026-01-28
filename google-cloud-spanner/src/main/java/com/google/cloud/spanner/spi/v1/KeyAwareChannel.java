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
import com.google.spanner.v1.CommitRequest;
import com.google.spanner.v1.ExecuteSqlRequest;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.ReadRequest;
import com.google.spanner.v1.ResultSet;
import com.google.spanner.v1.RollbackRequest;
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
import javax.annotation.Nullable;

/**
 * ManagedChannel that routes eligible requests using location-aware routing hints.
 *
 * <p>Routing is applied only to streaming read and streaming query methods.
 */
@InternalApi
final class KeyAwareChannel extends ManagedChannel {
  private static final String STREAMING_READ_METHOD = "google.spanner.v1.Spanner/StreamingRead";
  private static final String STREAMING_SQL_METHOD =
      "google.spanner.v1.Spanner/ExecuteStreamingSql";
  private static final String UNARY_SQL_METHOD = "google.spanner.v1.Spanner/ExecuteSql";
  private static final String COMMIT_METHOD = "google.spanner.v1.Spanner/Commit";
  private static final String ROLLBACK_METHOD = "google.spanner.v1.Spanner/Rollback";

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

  static KeyAwareChannel create(InstantiatingGrpcChannelProvider channelProvider)
      throws IOException {
    return new KeyAwareChannel(channelProvider, null);
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
          finder = new ChannelFinder(endpointCache, databaseId);
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

  private void recordAffinity(ByteString transactionId, @Nullable ChannelEndpoint endpoint) {
    if (transactionId == null || transactionId.isEmpty() || endpoint == null) {
      return;
    }
    String address = endpoint.getAddress();
    if (defaultEndpointAddress.equals(address)) {
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sendMessage(RequestT message) {
      ChannelEndpoint endpoint = null;
      ChannelFinder finder = null;

      if (message instanceof ReadRequest) {
        ReadRequest.Builder reqBuilder = ((ReadRequest) message).toBuilder();
        String databaseId = parentChannel.extractDatabaseIdFromSession(reqBuilder.getSession());
        ByteString transactionId = transactionIdFromSelector(reqBuilder.getTransaction());
        endpoint = parentChannel.affinityEndpoint(transactionId);
        if (databaseId != null) {
          finder = parentChannel.getOrCreateChannelFinder(databaseId);
          ChannelEndpoint routed = finder.findServer(reqBuilder);
          if (endpoint == null) {
            endpoint = routed;
          }
        }
        message = (RequestT) reqBuilder.build();
      } else if (message instanceof ExecuteSqlRequest) {
        ExecuteSqlRequest.Builder reqBuilder = ((ExecuteSqlRequest) message).toBuilder();
        String databaseId = parentChannel.extractDatabaseIdFromSession(reqBuilder.getSession());
        ByteString transactionId = transactionIdFromSelector(reqBuilder.getTransaction());
        endpoint = parentChannel.affinityEndpoint(transactionId);
        if (databaseId != null) {
          finder = parentChannel.getOrCreateChannelFinder(databaseId);
          ChannelEndpoint routed = finder.findServer(reqBuilder);
          if (endpoint == null) {
            endpoint = routed;
          }
        }
        message = (RequestT) reqBuilder.build();
      } else if (message instanceof CommitRequest) {
        CommitRequest request = (CommitRequest) message;
        if (!request.getTransactionId().isEmpty()) {
          endpoint = parentChannel.affinityEndpoint(request.getTransactionId());
          transactionIdToClear = request.getTransactionId();
        }
      } else if (message instanceof RollbackRequest) {
        RollbackRequest request = (RollbackRequest) message;
        if (!request.getTransactionId().isEmpty()) {
          endpoint = parentChannel.affinityEndpoint(request.getTransactionId());
          transactionIdToClear = request.getTransactionId();
        }
      } else {
        throw new IllegalStateException(
            "Only read, query, commit, and rollback requests are supported for key-aware calls.");
      }

      if (endpoint == null) {
        endpoint = parentChannel.endpointCache.defaultChannel();
      }
      selectedEndpoint = endpoint;
      this.channelFinder = finder;

      delegate = endpoint.getChannel().newCall(methodDescriptor, callOptions);
      delegate.start(responseListener, headers);
      delegate.sendMessage(message);
    }

    @Override
    public void halfClose() {
      if (delegate != null) {
        delegate.halfClose();
      } else {
        throw new IllegalStateException("halfClose called before sendMessage");
      }
    }

    @Override
    public void cancel(@Nullable String message, @Nullable Throwable cause) {
      if (delegate != null) {
        delegate.cancel(message, cause);
      } else if (responseListener != null) {
        responseListener.onClose(
            io.grpc.Status.CANCELLED.withDescription(message).withCause(cause), new Metadata());
      }
    }

    void maybeRecordAffinity(ByteString transactionId) {
      parentChannel.recordAffinity(transactionId, selectedEndpoint);
    }

    void maybeClearAffinity() {
      parentChannel.clearAffinity(transactionIdToClear);
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
      if (message instanceof PartialResultSet) {
        PartialResultSet response = (PartialResultSet) message;
        if (response.hasCacheUpdate() && call.channelFinder != null) {
          call.channelFinder.update(response.getCacheUpdate());
        }
        ByteString transactionId = transactionIdFromMetadata(response);
        if (transactionId != null) {
          call.maybeRecordAffinity(transactionId);
        }
      } else if (message instanceof ResultSet) {
        ResultSet response = (ResultSet) message;
        ByteString transactionId = transactionIdFromMetadata(response);
        if (transactionId != null) {
          call.maybeRecordAffinity(transactionId);
        }
      }
      super.onMessage(message);
    }

    @Override
    public void onClose(io.grpc.Status status, Metadata trailers) {
      call.maybeClearAffinity();
      super.onClose(status, trailers);
    }
  }
}
