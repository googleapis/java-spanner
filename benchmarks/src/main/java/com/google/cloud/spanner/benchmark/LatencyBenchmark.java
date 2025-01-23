/*
 * Copyright 2024 Google LLC
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

package com.google.cloud.spanner.benchmark;

import static com.google.common.math.Quantiles.percentiles;

import com.google.api.core.InternalApi;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.benchmark.BenchmarkRunner.TransactionType;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

@InternalApi
@VisibleForTesting
public class LatencyBenchmark {
  public static void main(String[] args) throws ParseException {
    CommandLine cmd = parseCommandLine(args);
    String project = System.getenv("SPANNER_CLIENT_BENCHMARK_GOOGLE_CLOUD_PROJECT");
    String instance = System.getenv("SPANNER_CLIENT_BENCHMARK_SPANNER_INSTANCE");
    String database = System.getenv("SPANNER_CLIENT_BENCHMARK_SPANNER_DATABASE");
    String fullyQualifiedDatabase;

    if (project != null && instance != null && database != null) {
      fullyQualifiedDatabase =
          String.format("projects/%s/instances/%s/databases/%s", project, instance, database);
    } else {
      throw new IllegalArgumentException(
          "You must either set all the environment variables SPANNER_CLIENT_BENCHMARK_GOOGLE_CLOUD_PROJECT, SPANNER_CLIENT_BENCHMARK_SPANNER_INSTANCE and SPANNER_CLIENT_BENCHMARK_SPANNER_DATABASE, or specify a value for the command line argument --database");
    }

    LatencyBenchmark benchmark = new LatencyBenchmark(DatabaseId.of(fullyQualifiedDatabase));
    benchmark.run(cmd);
  }

  private static CommandLine parseCommandLine(String[] args) throws ParseException {
    Options options = new Options();
    options.addOption("d", "database", true, "The database to use for benchmarking.");
    options.addOption(
        "c", "clients", true, "The number of clients that will be executing queries in parallel.");
    options.addOption(
        "o",
        "operations",
        true,
        "The number of operations that each client will execute. Defaults to 1000.");
    options.addOption(
        "w",
        "wait",
        true,
        "The wait time in milliseconds between each query that is executed by each client. Defaults to 0. "
            + "Set this to for example 1000 to have each client execute 1 query per second.");
    options.addOption(
        "t",
        "transaction",
        true,
        "The type of transaction to execute. Must be either READ_ONLY or READ_WRITE. Defaults to READ_ONLY.");
    options.addOption("m", "multiplexed", true, "Use multiplexed sessions. Defaults to false.");
    options.addOption("w", "wait", true, "Wait time in millis. Defaults to zero.");
    options.addOption("name", true, "Name of this test run");
    options.addOption("wu", true, "Warm up time in minutes. Defaults to 2 minutes");
    options.addOption("sr", true, "Stale Read in seconds. Defaults to 0 seconds");
    CommandLineParser parser = new DefaultParser();
    return parser.parse(options, args);
  }

  private final DatabaseId databaseId;

  LatencyBenchmark(DatabaseId databaseId) {
    this.databaseId = databaseId;
  }

  public void run(CommandLine commandLine) {
    int clients =
        commandLine.hasOption('c') ? Integer.parseInt(commandLine.getOptionValue('c')) : 16;
    int operations =
        commandLine.hasOption('o') ? Integer.parseInt(commandLine.getOptionValue('o')) : 1000;
    int waitMillis =
        commandLine.hasOption('w') ? Integer.parseInt(commandLine.getOptionValue('w')) : 0;
    TransactionType transactionType =
        commandLine.hasOption('t')
            ? TransactionType.valueOf(commandLine.getOptionValue('t').toUpperCase(Locale.ENGLISH))
            : TransactionType.READ_ONLY_SINGLE_USE;
    boolean useMultiplexedSession =
        commandLine.hasOption('m') ? Boolean.parseBoolean(commandLine.getOptionValue('m')) : false;
    int warmUpMinutes =
        commandLine.hasOption("wu") ? Integer.parseInt(commandLine.getOptionValue("wu")) : 2;
    int staleReadSeconds =
        commandLine.hasOption("sr") ? Integer.parseInt(commandLine.getOptionValue("sr")) : 0;

    System.out.println();
    System.out.println("Running benchmark with the following options");
    System.out.printf("Database: %s\n", databaseId);
    System.out.printf("Clients: %d\n", clients);
    System.out.printf("Number of Minutes: %dm\n", operations);
    System.out.printf("Transaction type: %s\n", transactionType);
    System.out.printf("Use Multiplexed Sessions: %s\n", useMultiplexedSession);
    System.out.printf("Wait between queries: %dms\n", waitMillis);
    System.out.printf("Warm Up Minutes: %dm\n", warmUpMinutes);
    System.out.printf("Stale Read Seconds: %ds\n", staleReadSeconds);

    System.out.println("Running benchmarking with skipping gRPC");
    System.out.println();
    System.out.println("Running benchmark for Java Client Library");
    JavaClientRunner javaClientRunner = new JavaClientRunner(databaseId);
    javaClientRunner.execute(
        transactionType,
        clients,
        operations,
        waitMillis,
        useMultiplexedSession,
        warmUpMinutes,
        staleReadSeconds);
  }

  public static void printResults(String header, List<Integer> results) {
    if (results == null) {
      return;
    }
    List<Integer> orderedResults = new ArrayList<>(results);
    Collections.sort(orderedResults);
    System.out.println();
    System.out.println(header);
    System.out.printf("Total number of queries: %d\n", orderedResults.size());
    System.out.printf("Avg: %.2fµs\n", avg(results));
    System.out.printf("P50: %.2fµs\n", percentile(50, orderedResults));
    System.out.printf("P95: %.2fµs\n", percentile(95, orderedResults));
    System.out.printf("P99: %.2fµs\n", percentile(99, orderedResults));
  }

  private static double percentile(int percentile, List<Integer> orderedResults) {
    return percentiles().index(percentile).compute(orderedResults) / 1_000.0f;
  }

  private static double avg(List<Integer> results) {
    return results.stream().collect(Collectors.averagingDouble(result -> result / 1_000.0f));
  }
}
