package com.google.cloud.spanner.spi.v1;

import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.spanner.v1.PartialResultSet;
import com.google.spanner.v1.ReadRequest;
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
 * KeyAwareChannel is a ManagedChannel that intercepts calls to key-aware Spanner methods, primarily
 * StreamingRead. It uses a ChannelFinder to select the appropriate server based on the request's
 * key information. The ChannelFinder's cache is updated with information received in response
 * headers.
 */
final class KeyAwareChannel extends ManagedChannel {
  private final ManagedChannel defaultChannel; // The original channel from the builder
  private final GrpcChannelFinderServerFactory serverFactory;
  private final String authority; // Authority from the original channel
  private final String deployment; // Global deployment ID, derived from endpoint
  private final Map<String, SoftReference<ChannelFinder>> channelFinders =
      new ConcurrentHashMap<>();

  private KeyAwareChannel(InstantiatingGrpcChannelProvider.Builder channelBuilder)
      throws IOException {
    this.serverFactory = new GrpcChannelFinderServerFactory(channelBuilder);
    this.defaultChannel = this.serverFactory.defaultServer().getChannel();
    this.authority = this.defaultChannel.authority();
    // Use the builder's original endpoint as the deployment identifier
    this.deployment = channelBuilder.build().getEndpoint();
  }

  static KeyAwareChannel create(InstantiatingGrpcChannelProvider.Builder channelBuilder)
      throws IOException {
    return new KeyAwareChannel(channelBuilder);
  }

  private String extractDatabaseIdFromSession(String session) {
    if (session == null || session.isEmpty()) {
      return null;
    }
    // Session format:
    // projects/{project}/instances/{instance}/databases/{database}/sessions/{session_id}
    // Database ID: projects/{project}/instances/{instance}/databases/{database}
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
      synchronized (channelFinders) { // Synchronize to prevent duplicate creation
        // Double-check after acquiring lock
        ref = channelFinders.get(databaseId);
        finder = (ref != null) ? ref.get() : null;
        if (finder == null) {
          // The databaseId (e.g., projects/../databases/DB_NAME) is used as the databaseUri
          finder = new ChannelFinder(this.serverFactory, this.deployment, databaseId);
          channelFinders.put(databaseId, new SoftReference<>(finder));
        }
      }
    }
    return finder;
  }

  @Override
  public ManagedChannel shutdownNow() {
    // TODO: Need to manage shutdown of all created channels in serverFactory
    // and clear channelFinders map, potentially shutting down individual finders/channels.
    return this;
  }

  @Override
  public ManagedChannel shutdown() {
    // TODO: Need to manage shutdown of all created channels in serverFactory
    return this;
  }

  @Override
  public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(
      MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
    if (isKeyAware(methodDescriptor)) {
      return new KeyAwareClientCall<>(this, methodDescriptor, callOptions);
    }
    return defaultChannel.newCall(methodDescriptor, callOptions);
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

  // Determines if a method is key-aware (e.g., StreamingRead)
  boolean isKeyAware(MethodDescriptor<?, ?> methodDescriptor) {
    return "google.spanner.v1.Spanner/StreamingRead".equals(methodDescriptor.getFullMethodName());
  }

  static class KeyAwareClientCall<RequestT, ResponseT>
      extends ForwardingClientCall<RequestT, ResponseT> {
    private final KeyAwareChannel parentChannel;
    private final MethodDescriptor<RequestT, ResponseT> methodDescriptor;
    private final CallOptions callOptions;
    private Listener<ResponseT> responseListener;
    private Metadata headers;
    @Nullable private ClientCall<RequestT, ResponseT> delegate;
    private ChannelFinder channelFinder; // Set in sendMessage

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
        // This should not happen in normal flow as sendMessage initializes the delegate.
        // If it does, it means a method like halfClose() or cancel() was called before
        // sendMessage().
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
    public void sendMessage(RequestT message) {
      ChannelFinderServer server = null;

      if (message instanceof ReadRequest) {
        ReadRequest.Builder reqBuilder = ((ReadRequest) message).toBuilder();
        String databaseId = parentChannel.extractDatabaseIdFromSession(reqBuilder.getSession());

        if (databaseId == null) {
          server = parentChannel.serverFactory.defaultServer();
        } else {
          this.channelFinder = parentChannel.getOrCreateChannelFinder(databaseId);
          server = this.channelFinder.findServer(reqBuilder);
          message = (RequestT) reqBuilder.build(); // Apply routing info changes
          System.out.println("DEBUG: Routing hint: " + ((ReadRequest) message).getRoutingHint());
        }
      } else {
        // Other types of requests should never be passed to KeyAwareClientCall to begin with.
        throw new IllegalStateException("Only ReadRequest is supported for key-aware calls.");
      }

      delegate = server.getChannel().newCall(methodDescriptor, callOptions);
      delegate.start(responseListener, headers);
      delegate.sendMessage(message);
    }

    @Override
    public void halfClose() {
      if (delegate != null) {
        delegate.halfClose();
      } else {
        // Handle the case where sendMessage was never called, though this is unlikely
        // in normal gRPC client flows.
        throw new IllegalStateException("halfClose called before sendMessage");
      }
    }

    @Override
    public void cancel(@Nullable String message, @Nullable Throwable cause) {
      if (delegate != null) {
        delegate.cancel(message, cause);
      } else {
        // If cancel is called before sendMessage, there's no delegate to cancel.
        // The listener's onClosed can be invoked to signal termination.
        if (responseListener != null) {
          responseListener.onClose(
              io.grpc.Status.CANCELLED.withDescription(message).withCause(cause), new Metadata());
        }
      }
    }
  }

  static class KeyAwareClientCallListener<ResponseT>
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
      }
      super.onMessage(message);
    }
  }
}
