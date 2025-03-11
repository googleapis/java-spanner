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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import spanner.experimental.AccessToken;
import spanner.experimental.FinalScramRequest;
import spanner.experimental.FinalScramResponse;
import spanner.experimental.InitialScramRequest;
import spanner.experimental.InitialScramResponse;
import spanner.experimental.LoginRequest;
import spanner.experimental.LoginResponse;
import spanner.experimental.LoginServiceGrpc;

public class Scram {

  private final String username;
  private final String password;
  private final byte[] clientNonce;
  private final X509Certificate certificate;
  private byte[] saltedPassword;
  private byte[] authMessage;
  private byte[] serverNonce;
  private byte[] salt;
  private int iterationCount;
  private GrpcClient grpcClient;

  private static final int ARGON_MEMORY_LIMIT = 64 * 1024;
  private static final int ARGON_THREADS = 4;
  private static final int ARGON_KEY_LENGTH = 32;
  private static final String CLIENT_KEY_MESSAGE = "Client Key";
  private static final String SERVER_KEY_MESSAGE = "Server Key";
  private static final String HMAC_SHA256 = "HmacSHA256";
  private static final String SHA256 = "SHA-256";
  private static final int NONCE_LENGTH = 16;

  private final LoginServiceGrpc.LoginServiceStub loginService;

  public Scram(String username, String password, GrpcClient grpcClient){
    this.username = username;
    this.password = password;
    this.certificate = grpcClient.getServerCertificate();
    this.clientNonce = nonce();
    this.loginService = grpcClient.getLoginService();
    this.grpcClient = grpcClient;
  }

  public AccessToken login() throws Exception {
    InitialScramRequest initialScramRequest =
        InitialScramRequest.newBuilder().setClientNonce(ByteString.copyFrom(clientNonce)).build();

    final LoginRequest initialLoginRequest =
        LoginRequest.newBuilder()
            .setUsername(username)
            .setInitialScramRequest(initialScramRequest)
            .build();

    AccessToken[] accessToken = new AccessToken[1];
    final StreamObserver<LoginRequest>[] requestObserverContainer = new StreamObserver[1];
    final CountDownLatch latch = new CountDownLatch(1);

    requestObserverContainer[0] =
        loginService.login(
            new StreamObserver<LoginResponse>() {
              @Override
              public void onNext(LoginResponse loginResponse) {
                try {
                  if (loginResponse.hasInitialScramResponse()) {
                    addServerFirstMessage(loginResponse.getInitialScramResponse());
                    byte[] clientProof =
                        clientProof(
                            initialLoginRequest.getInitialScramRequest(),
                            loginResponse.getInitialScramResponse());

                    LoginRequest finalLoginRequest =
                        LoginRequest.newBuilder()
                            .setUsername(username)
                            .setFinalScramRequest(
                                FinalScramRequest.newBuilder()
                                    .setCredential(ByteString.copyFrom(clientProof))
                                    .build())
                            .build();
                    requestObserverContainer[0].onNext(finalLoginRequest);

                  } else {
                    verifyLoginResponse(loginResponse.getFinalScramResponse());
                    accessToken[0] = loginResponse.getAccessToken();
                    latch.countDown(); // Signal completion
                  }
                } catch (Exception e) {
                  System.err.println("Exception in onNext: " + e.getMessage());
                  requestObserverContainer[0].onError(e);
                }
              }

              @Override
              public void onError(Throwable t) {
                System.err.println("Error during login: " + t.getMessage());
                latch.countDown();
              }

              @Override
              public void onCompleted() {
                requestObserverContainer[0].onCompleted();
              }
            });

    requestObserverContainer[0].onNext(initialLoginRequest);

    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new Exception("Login process interrupted", e);
    } finally {
      grpcClient.close();
    }

    return accessToken[0];
  }

  private void addServerFirstMessage(InitialScramResponse initialScramResponse) throws Exception {
    serverNonce = initialScramResponse.getServerNonce().toByteArray();
    salt = initialScramResponse.getSalt().toByteArray();
    iterationCount = initialScramResponse.getIterationCount();
    if (salt.length == 0) {
      throw new Exception("No salt found in the response");
    }
    if (iterationCount == 0) {
      throw new Exception("No iteration count found in response");
    }
  }

  private void verifyLoginResponse(FinalScramResponse finalScramResponse) throws Exception {
    byte[] serverKey = serverKey(saltedPassword);
    byte[] serverSignature = serverSignature(serverKey, authMessage);
    byte[] candidateServerSignature = finalScramResponse.getServerSignature().toByteArray();
    if (!Arrays.equals(serverSignature, candidateServerSignature)) {
      throw new Exception("Server signature does not match");
    }
  }

  private byte[] clientProof(
      InitialScramRequest initialScramRequest, InitialScramResponse initialScramResponse)
      throws Exception {
    saltedPassword =
        saltPassword(
            password,
            initialScramResponse.getSalt().toByteArray(),
            initialScramResponse.getIterationCount());
    byte[] clientKey = clientKey(saltedPassword);
    byte[] storedKey = storedKey(clientKey);
    authMessage =
        authMessage(
            username, initialScramRequest, initialScramResponse, certificate.getSignature());
    byte[] clientSignature = clientSignature(storedKey, authMessage);
    return xorBytes(clientKey, clientSignature);
  }

  @VisibleForTesting
  public static byte[] saltPassword(String password, byte[] salt, int iterationCount)
      throws Exception {
    if (salt.length == 0) {
      throw new Exception("No salt found");
    }
    if (iterationCount == 0) {
      throw new Exception("No iteration count found");
    }
    Argon2Parameters parameters =
        new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withIterations(iterationCount)
            .withMemoryAsKB(ARGON_MEMORY_LIMIT)
            .withParallelism(ARGON_THREADS)
            .build();
    Argon2BytesGenerator argon2BytesGenerator = new Argon2BytesGenerator();
    argon2BytesGenerator.init(parameters);
    byte[] saltedPassword = new byte[ARGON_KEY_LENGTH];
    argon2BytesGenerator.generateBytes(
        password.getBytes(StandardCharsets.UTF_8), saltedPassword, 0, saltedPassword.length);

    return saltedPassword;
  }

  @VisibleForTesting
  public static byte[] nonce() {
    byte[] nonce = new byte[NONCE_LENGTH];
    SecureRandom random = new SecureRandom();
    random.nextBytes(nonce);
    return nonce;
  }

  private static byte[] xorBytes(byte[] a, byte[] b) {
    int minLength = Math.min(a.length, b.length);
    byte[] result = new byte[minLength];
    for (int i = 0; i < minLength; i++) {
      result[i] = (byte) (a[i] ^ b[i]);
    }
    return result;
  }

  private static byte[] clientSignature(byte[] storedKey, byte[] authMessage) throws Exception {
    try {
      Mac hmac = Mac.getInstance(HMAC_SHA256);
      hmac.init(new SecretKeySpec(storedKey, HMAC_SHA256));
      hmac.update(authMessage);
      return hmac.doFinal();
    } catch (java.security.GeneralSecurityException e) {
      throw new Exception("Failed to generate client signature due to: " + e.getMessage(), e);
    }
  }

  private static byte[] clientKey(byte[] saltedPassword) throws Exception {
    return hmacSha256(saltedPassword, CLIENT_KEY_MESSAGE.getBytes());
  }

  private static byte[] serverKey(byte[] saltedPassword) throws Exception {
    return hmacSha256(saltedPassword, SERVER_KEY_MESSAGE.getBytes());
  }

  private static byte[] hmacSha256(byte[] key, byte[] message) throws Exception {
    try {
      Mac hmac = Mac.getInstance(HMAC_SHA256);
      hmac.init(new SecretKeySpec(key, HMAC_SHA256));
      hmac.update(message);
      return hmac.doFinal();
    } catch (java.security.GeneralSecurityException e) {
      throw new Exception("Failed to create hmac-sha256 due to: " + e.getMessage(), e);
    }
  }

  private static byte[] serverSignature(byte[] serverKey, byte[] authMessage) throws Exception {
    return hmacSha256(serverKey, authMessage);
  }

  private static byte[] storedKey(byte[] clientKey) throws Exception {
    try {
      MessageDigest digest = MessageDigest.getInstance(SHA256);
      return digest.digest(clientKey);
    } catch (NoSuchAlgorithmException e) {
      throw new Exception("Failed to create stored key due to: " + e.getMessage(), e);
    }
  }

  private static byte[] authMessage(
      String username,
      InitialScramRequest initialScramRequest,
      InitialScramResponse initialScramResponse,
      byte[] certificateSignature) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      outputStream.write(username.getBytes(StandardCharsets.UTF_8));
      outputStream.write(',');
      outputStream.write(initialScramRequest.getClientNonce().toByteArray());
      outputStream.write(',');
      outputStream.write(initialScramRequest.getClientNonce().toByteArray());
      outputStream.write(',');
      outputStream.write(initialScramResponse.getServerNonce().toByteArray());
      outputStream.write(',');
      outputStream.write(initialScramResponse.getSalt().toByteArray());
      outputStream.write(',');
      outputStream.write(
          String.valueOf(initialScramResponse.getIterationCount())
              .getBytes(StandardCharsets.UTF_8));
      outputStream.write(',');
      outputStream.write(certificateSignature);
      outputStream.write(',');
      outputStream.write(initialScramRequest.getClientNonce().toByteArray());
      outputStream.write(',');
      outputStream.write(initialScramResponse.getServerNonce().toByteArray());
    } catch (IOException e) {
      throw new RuntimeException("Failed to construct authMessage", e);
    }
    return outputStream.toByteArray();
  }
}
