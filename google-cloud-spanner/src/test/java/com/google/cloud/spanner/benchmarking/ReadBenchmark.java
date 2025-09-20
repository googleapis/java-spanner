package com.google.cloud.spanner.benchmarking;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
public class ReadBenchmark {

  @State(Scope.Benchmark)
  public static class BenchmarkState {

    Spanner spanner;
    DatabaseClient databaseClient;

    @Setup(Level.Trial)
    public void setup() {
      System.out.println("Setup");
      spanner = SpannerOptions.newBuilder()
          .setProjectId("span-cloud-testing")
          .build().getService();
      databaseClient = spanner.getDatabaseClient(
          DatabaseId.of("span-cloud-testing", "sakthi-spanner-testing", "benchmarking"));
    }

    @TearDown(Level.Trial)
    public void tearDown() {
      spanner.close();
      System.out.println("Tear down");
    }
  }

  @Benchmark
  @Warmup(time = 10, timeUnit = TimeUnit.SECONDS, iterations = 1)
  @Measurement(time = 10, timeUnit = TimeUnit.SECONDS, iterations = 1)
  public void staleReadBenchmark(BenchmarkState benchmarkState) {
    List<String> columns = new ArrayList<>();
    columns.add("id");
    columns.add("name");
    try (ReadContext readContext = benchmarkState.databaseClient.singleUse()) {
      try (ResultSet resultSet = readContext.read("Employees",
          KeySet.singleKey(Key.of("2")), columns)) {
        while (resultSet.next()) {
          System.out.println(resultSet.getLong("id"));
        }
      }
    }
  }

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(ReadBenchmark.class.getSimpleName())
        .result("my_benchmark_results.json")
        .resultFormat(ResultFormatType.JSON)
        .build();
    new Runner(opt).run();
  }
}
