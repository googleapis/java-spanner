/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.executor.spanner;

import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerExceptionFactory;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Worker proxy for Java API. This is the main entry of the Java client proxy on cloud Spanner Java
 * client.
 */
public class WorkerProxy {

  private static final Logger LOGGER = Logger.getLogger(WorkerProxy.class.getName());

  private static final String OPTION_SPANNER_PORT = "spanner_port";
  private static final String OPTION_PORT = "port";
  private static final String OPTION_CERTIFICATE = "cert";
  private static final String OPTION_KEY = "key";
  private static final String OPTION_USE_PLAIN_TEXT_CHANNEL = "use_plain_text_channel";
  private static final String OPTION_ENABLE_GRPC_FAULT_INJECTOR = "enable_grpc_fault_injector";

  public static int spannerPort = 0;
  public static int port = 0;
  public static String cert = "";
  public static String key = "";
  public static boolean usePlainTextChannel = false;
  public static boolean enableGrpcFaultInjector = false;

  public static CommandLine commandLine;

  private static final int MIN_PORT = 0, MAX_PORT = 65535;

  public static void main(String[] args) throws Exception {
    commandLine = buildOptions(args);

    if (!commandLine.hasOption(OPTION_SPANNER_PORT)) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT,
          "Spanner port need to be assigned in order to start worker proxy.");
    }
    spannerPort = Integer.parseInt(commandLine.getOptionValue(OPTION_SPANNER_PORT));
    if (spannerPort < MIN_PORT || spannerPort > MAX_PORT) {
      throw new IllegalArgumentException(
          "Spanner port must be between " + MIN_PORT + " and " + MAX_PORT);
    }

    if (!commandLine.hasOption(OPTION_PORT)) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT, "Port need to be assigned in order to start worker proxy.");
    }
    port = Integer.parseInt(commandLine.getOptionValue(OPTION_PORT));
    if (port < MIN_PORT || port > MAX_PORT) {
      throw new IllegalArgumentException("Port must be between " + MIN_PORT + " and " + MAX_PORT);
    }

    if (!commandLine.hasOption(OPTION_CERTIFICATE)) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT,
          "Certificate need to be assigned in order to start worker proxy.");
    }
    cert = commandLine.getOptionValue(OPTION_CERTIFICATE);
    key = commandLine.getOptionValue(OPTION_KEY);

    usePlainTextChannel = commandLine.hasOption(OPTION_USE_PLAIN_TEXT_CHANNEL);
    enableGrpcFaultInjector = commandLine.hasOption(OPTION_ENABLE_GRPC_FAULT_INJECTOR);

    Server server;
    while (true) {
      try {
        CloudExecutorImpl cloudExecutorImpl = new CloudExecutorImpl(enableGrpcFaultInjector);
        HealthStatusManager healthStatusManager = new HealthStatusManager();
        // Set up Cloud server.
        server =
            ServerBuilder.forPort(port)
                .addService(cloudExecutorImpl)
                .addService(ProtoReflectionService.newInstance())
                .addService(healthStatusManager.getHealthService())
                .build();
        server.start();
        LOGGER.log(Level.INFO, String.format("Server started on port: %d", port));
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, String.format("Failed to start server on port %d", port), e);
        continue; // We did not bind in time.  Try another port.
      }
      break;
    }
    server.awaitTermination();
  }

  private static CommandLine buildOptions(String[] args) {
    Options options = new Options();

    options.addOption(
        null, OPTION_SPANNER_PORT, true, "Port of Spanner Frontend to which to send requests.");
    options.addOption(null, OPTION_PORT, true, "Port to start worker proxy on.");
    options.addOption(
        null, OPTION_CERTIFICATE, true, "Certificate used to connect to Spanner GFE.");
    options.addOption(null, OPTION_KEY, true, "Service key file used to set authentication.");
    options.addOption(
        null,
        OPTION_USE_PLAIN_TEXT_CHANNEL,
        false,
        "Use a plain text gRPC channel (intended for the Cloud Spanner Emulator).");
    options.addOption(
        null,
        OPTION_ENABLE_GRPC_FAULT_INJECTOR,
        false,
        "Enable grpc fault injector in cloud client executor.");

    CommandLineParser parser = new DefaultParser();
    try {
      return parser.parse(options, args);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }
}
