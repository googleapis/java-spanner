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

package com.google.cloud;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import spanner.experimental.LoginServiceGrpc;

public class GrpcClient {

  private final ManagedChannel channel;
  private final LoginServiceGrpc.LoginServiceStub loginService;
  private X509Certificate serverCertificate; // Store the server certificate
  private final int DEFAULT_PORT = 15000;

  public GrpcClient(String endpoint) throws IOException {
    this(endpoint, null, null);
  }

  public GrpcClient(String endpoint, String clientCertificate, String clientCertificateKey) {
    try {
      URI uri = new URI(endpoint);
      String host = uri.getHost();
      int port = (uri.getPort() == -1) ? DEFAULT_PORT : uri.getPort();

      if (host == null) {
        throw new IllegalArgumentException("Invalid endpoint: " + endpoint);
      }

      // Retrieve the server certificate during handshake
      this.serverCertificate = fetchServerCertificate(host, port);

      if (clientCertificate != null && clientCertificateKey != null) {
        this.channel =
            NettyChannelBuilder.forAddress(host, port)
                .sslContext(
                    GrpcSslContexts.forClient()
                        .keyManager(
                            new File(clientCertificate),
                            new File(clientCertificateKey)) // Client auth
                        .build())
                .build();
      } else {
        // Normal TLS
        ChannelCredentials credentials = TlsChannelCredentials.newBuilder().build();
        this.channel = Grpc.newChannelBuilderForAddress(host, port, credentials).build();
      }
      this.loginService = LoginServiceGrpc.newStub(channel);
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Establish a TLS connection to fetch the server certificate. */
  private X509Certificate fetchServerCertificate(String host, int port) throws IOException {
    try {
      SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
      try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(host, port)) {
        socket.startHandshake();
        Certificate[] serverCerts = socket.getSession().getPeerCertificates();
        if (serverCerts.length > 0 && serverCerts[0] instanceof X509Certificate) {
          return (X509Certificate) serverCerts[0]; // Store the first certificate
        }
      }
    } catch (SSLPeerUnverifiedException e) {
      throw new IOException("Failed to verify server certificate: " + e.getMessage(), e);
    }
    throw new IOException("No server certificate found");
  }

  public LoginServiceGrpc.LoginServiceStub getLoginService() {
    return loginService;
  }

  public X509Certificate getServerCertificate() {
    return serverCertificate;
  }

  public void close() {
    if (channel != null) {
      channel.shutdown();
    }
  }
}
