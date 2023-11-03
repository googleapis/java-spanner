/*
 * Copyright 2023 Google LLC
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

import com.google.common.base.Suppliers;
import io.grpc.Attributes;
import io.grpc.ClientStreamTracer;
import io.grpc.Metadata;
import io.grpc.Status;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

class SpannerClientStreamTracer extends ClientStreamTracer {
  private static final Logger logger = Logger.getLogger(SpannerClientStreamTracer.class.getName());

  private static final AtomicLong counter = new AtomicLong();

  /** Generated id for this tracer making it possible to relate multiple log messages. */
  private final long id;

  /** The request that is being executed. */
  private final Object request;

  /** The gRPC stream info of the attempt. */
  private final StreamInfo info;

  SpannerClientStreamTracer(Object request, StreamInfo info, Metadata headers) {
    this.id = counter.incrementAndGet();
    this.request = request;
    this.info = info;

    if (info.isTransparentRetry()) {
      logger.log(
          Level.FINE,
          formatLogMessage(
              () ->
                  String.format(
                      "Creating SpannerClientStreamTracer for transparent retry %d of request %s",
                      info.getPreviousAttempts(), request)));
    } else if (info.getPreviousAttempts() > 0) {
      logger.log(
          Level.FINE,
          formatLogMessage(
              () ->
                  String.format(
                      "Creating SpannerClientStreamTracer for retry %d of request %s",
                      info.getPreviousAttempts(), request)));
    } else {
      logger.log(
          Level.FINER,
          formatLogMessage(
              () -> String.format("Creating SpannerClientStreamTracer for request %s", request)));
    }
    logger.log(
        Level.FINEST,
        formatLogMessage(
            () -> String.format("Creating SpannerClientStreamTracer for stream %s", info)));
    logger.log(
        Level.FINEST,
        formatLogMessage(
            () -> String.format("Creating SpannerClientStreamTracer with headers %s", headers)));
  }

  @Override
  public void streamCreated(Attributes transportAttrs, Metadata headers) {
    super.streamCreated(transportAttrs, headers);
    logger.log(
        Level.FINEST, formatLogMessage(Suppliers.ofInstance("Stream created on ready transport")));
  }

  @Override
  public void createPendingStream() {
    super.createPendingStream();
    logger.log(
        Level.FINEST,
        formatLogMessage(
            Suppliers.ofInstance(
                "Name resolution is completed and the connection starts getting established")));
  }

  @Override
  public void outboundHeaders() {
    super.outboundHeaders();
    logger.log(
        Level.FINEST,
        formatLogMessage(Suppliers.ofInstance("Headers has been sent to the socket")));
  }

  @Override
  public void inboundHeaders() {
    super.inboundHeaders();
    logger.log(
        Level.FINEST,
        formatLogMessage(Suppliers.ofInstance("Headers has been received from the server")));
  }

  @Override
  public void inboundTrailers(Metadata trailers) {
    super.inboundTrailers(trailers);
    logger.log(
        Level.FINEST,
        formatLogMessage(
            () ->
                String.format(
                    "Trailing metadata has been received from the server: %s", trailers)));
  }

  @Override
  public void streamClosed(Status status) {
    super.streamClosed(status);
    logger.log(
        Level.FINER,
        formatLogMessage(() -> String.format("Stream closed with status: %s", status)));
  }

  @Override
  public void outboundMessage(int seqNo) {
    super.outboundMessage(seqNo);
    logger.log(
        Level.FINEST,
        formatLogMessage(
            () -> String.format("Outbound message %d has been passed to the stream", seqNo)));
  }

  @Override
  public void inboundMessage(int seqNo) {
    super.inboundMessage(seqNo);
    logger.log(
        Level.FINEST,
        formatLogMessage(
            () -> String.format("Inbound message %d has been received by the stream", seqNo)));
  }

  @Override
  public void outboundMessageSent(int seqNo, long optionalWireSize, long optionalUncompressedSize) {
    super.outboundMessageSent(seqNo, optionalWireSize, optionalUncompressedSize);
    logger.log(
        Level.FINEST,
        formatLogMessage(
            () ->
                String.format(
                    "Outbound message %d with size %d has been serialized and sent to the transport",
                    seqNo, optionalWireSize)));
  }

  @Override
  public void inboundMessageRead(int seqNo, long optionalWireSize, long optionalUncompressedSize) {
    super.inboundMessageRead(seqNo, optionalWireSize, optionalUncompressedSize);
    logger.log(
        Level.FINEST,
        formatLogMessage(
            () ->
                String.format(
                    "Inbound message %d with size %d has been has been fully read from the transport",
                    seqNo, optionalWireSize)));
  }

  private Supplier<String> formatLogMessage(Supplier<String> message) {
    return () -> String.format("[%d] [%s] %s", id, Thread.currentThread().getName(), message.get());
  }
}
