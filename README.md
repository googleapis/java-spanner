# Google Cloud Spanner Client for Java

Java idiomatic client for [Cloud Spanner][product-docs].

[![Maven][maven-version-image]][maven-version-link]
![Stability][stability-image]

- [Product Documentation][product-docs]
- [Client Library Documentation][javadocs]


## Quickstart

If you are using Maven with [BOM][libraries-bom], add this to your pom.xml file:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.google.cloud</groupId>
      <artifactId>libraries-bom</artifactId>
      <version>26.39.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-spanner</artifactId>
  </dependency>

```

If you are using Maven without the BOM, add this to your dependencies:

<!-- {x-version-update-start:google-cloud-spanner:released} -->

```xml
<dependency>
  <groupId>com.google.cloud</groupId>
  <artifactId>google-cloud-spanner</artifactId>
  <version>6.67.0</version>
</dependency>

```

If you are using Gradle 5.x or later, add this to your dependencies:

```Groovy
implementation platform('com.google.cloud:libraries-bom:26.40.0')

implementation 'com.google.cloud:google-cloud-spanner'
```
If you are using Gradle without BOM, add this to your dependencies:

```Groovy
implementation 'com.google.cloud:google-cloud-spanner:6.68.1'
```

If you are using SBT, add this to your dependencies:

```Scala
libraryDependencies += "com.google.cloud" % "google-cloud-spanner" % "6.68.1"
```
<!-- {x-version-update-end} -->

## Authentication

See the [Authentication][authentication] section in the base directory's README.

## Authorization

The client application making API calls must be granted [authorization scopes][auth-scopes] required for the desired Cloud Spanner APIs, and the authenticated principal must have the [IAM role(s)][predefined-iam-roles] required to access GCP resources using the Cloud Spanner API calls.

## Getting Started

### Prerequisites

You will need a [Google Cloud Platform Console][developer-console] project with the Cloud Spanner [API enabled][enable-api].
You will need to [enable billing][enable-billing] to use Google Cloud Spanner.
[Follow these instructions][create-project] to get your project set up. You will also need to set up the local development environment by
[installing the Google Cloud Command Line Interface][cloud-cli] and running the following commands in command line:
`gcloud auth login` and `gcloud config set project [YOUR PROJECT ID]`.

### Installation and setup

You'll need to obtain the `google-cloud-spanner` library.  See the [Quickstart](#quickstart) section
to add `google-cloud-spanner` as a dependency in your code.

## About Cloud Spanner


[Cloud Spanner][product-docs] is a fully managed, mission-critical, 
relational database service that offers transactional consistency at global scale, 
schemas, SQL (ANSI 2011 with extensions), and automatic, synchronous replication 
for high availability.

Be sure to activate the Cloud Spanner API on the Developer's Console to
use Cloud Spanner from your project.

See the [Cloud Spanner client library docs][javadocs] to learn how to
use this Cloud Spanner Client Library.


#### Calling Cloud Spanner
Here is a code snippet showing a simple usage example. Add the following imports
at the top of your file:

```java
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;

```

Then, to make a query to Spanner, use the following code:
```java
// Instantiates a client
SpannerOptions options = SpannerOptions.newBuilder().build();
Spanner spanner = options.getService();
String instance = "my-instance";
String database = "my-database";
try {
  // Creates a database client
  DatabaseClient dbClient = spanner.getDatabaseClient(
    DatabaseId.of(options.getProjectId(), instance, database));
  // Queries the database
  try (ResultSet resultSet = dbClient.singleUse().executeQuery(Statement.of("SELECT 1"))) {
    // Prints the results
    while (resultSet.next()) {
      System.out.printf("%d\n", resultSet.getLong(0));
    }
  }
} finally {
  // Closes the client which will free up the resources used
  spanner.close();
}
```

#### Complete source code

In [DatabaseSelect.java](https://github.com/googleapis/google-cloud-java/tree/master/google-cloud-examples/src/main/java/com/google/cloud/examples/spanner/snippets/DatabaseSelect.java) we put together all the code shown above in a single program.

## Session Pool

The Cloud Spanner client maintains a session pool, as sessions are expensive to create and are
intended to be long-lived. The client automatically takes a session from the pool and uses this
executing queries and transactions.
See [Session Pool and Channel Pool Configuration](session-and-channel-pool-configuration.md)
for in-depth background information about sessions and gRPC channels and how these are handled in
the Cloud Spanner Java client.

## Metrics

### Available client-side metrics:

* `spanner/max_in_use_sessions`: This returns the maximum
  number of sessions that have been in use during the last maintenance window
  interval, so as to provide an indication of the amount of activity currently
  in the database.

* `spanner/max_allowed_sessions`: This shows the maximum
  number of sessions allowed.

* `spanner/num_sessions_in_pool`: This metric allows users to
  see instance-level and database-level data for the total number of sessions in
  the pool at this very moment.

* `spanner/num_acquired_sessions`: This metric allows
  users to see the total number of acquired sessions.

* `spanner/num_released_sessions`: This metric allows
  users to see the total number of released (destroyed) sessions.

* `spanner/get_session_timeouts`: This gives you an
  indication of the total number of get session timed-out instead of being
  granted (the thread that requested the session is placed in a wait queue where
  it waits until a session is released into the pool by another thread) due to
  pool exhaustion since the server process started.

* `spanner/gfe_latency`: This metric shows latency between
  Google's network receiving an RPC and reading back the first byte of the response.

* `spanner/gfe_header_missing_count`: This metric shows the
  number of RPC responses received without the server-timing header, most likely
  indicating that the RPC never reached Google's network.

### Instrument with OpenTelemetry

Cloud Spanner client supports [OpenTelemetry Metrics](https://opentelemetry.io/),
which gives insight into the client internals and aids in debugging/troubleshooting
production issues. OpenTelemetry metrics will provide you with enough data to enable you to
spot, and investigate the cause of any unusual deviations from normal behavior.

All Cloud Spanner Metrics are prefixed with `spanner/` and uses `cloud.google.com/java` as [Instrumentation Scope](https://opentelemetry.io/docs/concepts/instrumentation-scope/). The
metrics will be tagged with:
* `database`: the target database name.
* `instance_id`: the instance id of the target Spanner instance.
* `client_id`: the user defined database client id.

By default, the functionality is disabled. You need to add OpenTelemetry dependencies, enable OpenTelemetry metrics and must configure the OpenTelemetry with appropriate exporters at the startup of your application:

#### OpenTelemetry Dependencies
If you are using Maven, add this to your pom.xml file
```xml
<dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-sdk</artifactId>
      <version>{opentelemetry.version}</version>
</dependency>
<dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-sdk-metrics</artifactId>
      <version>{opentelemetry.version}</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
    <version>{opentelemetry.version}</version>
</dependency>
```
If you are using Gradle, add this to your dependencies
```Groovy
compile 'io.opentelemetry:opentelemetry-sdk:{opentelemetry.version}'
compile 'io.opentelemetry:opentelemetry-sdk-metrics:{opentelemetry.version}'
compile 'io.opentelemetry:opentelemetry-exporter-oltp:{opentelemetry.version}'
```

#### OpenTelemetry Configuration
By default, all metrics are disabled. To enable metrics and configure the OpenTelemetry follow below:

```java
// Enable OpenTelemetry metrics before injecting OpenTelemetry object.
SpannerOptions.enableOpenTelemetryMetrics();

SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
// Use Otlp exporter or any other exporter of your choice.
  .registerMetricReader(PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().build())
  .build())
  .build();

OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setMeterProvider(sdkMeterProvider)
        .build()

SpannerOptions options = SpannerOptions.newBuilder()
// Inject OpenTelemetry object via Spanner Options or register OpenTelemetry object as Global
  .setOpenTelemetry(openTelemetry)
  .build();

Spanner spanner = options.getService();
```

#### OpenTelemetry SQL Statement Tracing
The OpenTelemetry traces that are generated by the Java client include any request and transaction
tags that have been set. The traces can also include the SQL statements that are executed. Enable
this with the `enableExtendedTracing` option:

```
SpannerOptions options = SpannerOptions.newBuilder()
  .setOpenTelemetry(openTelemetry)
  .setEnableExtendedTracing(true)
  .build();
```

This option can also be enabled by setting the environment variable
`SPANNER_ENABLE_EXTENDED_TRACING=true`.

#### OpenTelemetry API Tracing
You can enable tracing of each API call that the Spanner client executes with the `enableApiTracing`
option. These traces also include any retry attempts for an API call:
  
```
SpannerOptions options = SpannerOptions.newBuilder()
.setOpenTelemetry(openTelemetry)
.setEnableApiTracing(true)
.build();
```
  
This option can also be enabled by setting the environment variable
`SPANNER_ENABLE_API_TRACING=true`.

> Note: The attribute keys that are used for additional information about retry attempts and the number of requests might change in a future release.

### Instrument with OpenCensus

> Note: OpenCensus project is deprecated. See [Sunsetting OpenCensus](https://opentelemetry.io/blog/2023/sunsetting-opencensus/).
We recommend migrating to OpenTelemetry, the successor project.

Cloud Spanner client supports [Opencensus Metrics](https://opencensus.io/stats/),
which gives insight into the client internals and aids in debugging/troubleshooting
production issues. OpenCensus metrics will provide you with enough data to enable you to
spot, and investigate the cause of any unusual deviations from normal behavior.

All Cloud Spanner Metrics are prefixed with `cloud.google.com/java/spanner` 

The metrics are tagged with:
* `database`: the target database name.
* `instance_id`: the instance id of the target Spanner instance.
* `client_id`: the user defined database client id.
* `library_version`: the version of the library that you're using.


By default, the functionality is disabled. You need to include opencensus-impl
dependency to collect the data and exporter dependency to export to backend.

[Click here](https://medium.com/google-cloud/troubleshooting-cloud-spanner-applications-with-opencensus-2cf424c4c590) for more information.

#### OpenCensus Dependencies

If you are using Maven, add this to your pom.xml file
```xml
<dependency>
  <groupId>io.opencensus</groupId>
  <artifactId>opencensus-impl</artifactId>
  <version>0.30.0</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.opencensus</groupId>
  <artifactId>opencensus-exporter-stats-stackdriver</artifactId>
  <version>0.30.0</version>
</dependency>
```
If you are using Gradle, add this to your dependencies
```Groovy
compile 'io.opencensus:opencensus-impl:0.30.0'
compile 'io.opencensus:opencensus-exporter-stats-stackdriver:0.30.0'
```

#### Configure the OpenCensus Exporter

At the start of your application configure the exporter:

```java
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
// Enable OpenCensus exporters to export metrics to Stackdriver Monitoring.
// Exporters use Application Default Credentials to authenticate.
// See https://developers.google.com/identity/protocols/application-default-credentials
// for more details.
// The minimum reporting period for Stackdriver is 1 minute.
StackdriverStatsExporter.createAndRegister();
```
#### Enable RPC Views

By default, all session metrics are enabled. To enable RPC views, use either of the following method:

```java
// Register views for GFE metrics, including gfe_latency and gfe_header_missing_count.
SpannerRpcViews.registerGfeLatencyAndHeaderMissingCountViews();

// Register GFE Latency view. 
SpannerRpcViews.registerGfeLatencyView();

// Register GFE Header Missing Count view.
SpannerRpcViews.registerGfeHeaderMissingCountView();
```

## Traces
Cloud Spanner client supports OpenTelemetry Traces, which gives insight into the client internals and aids in debugging/troubleshooting production issues. 

By default, the functionality is disabled. You need to add OpenTelemetry dependencies, enable OpenTelemetry traces and must configure the OpenTelemetry with appropriate exporters at the startup of your application.

#### OpenTelemetry Dependencies

If you are using Maven, add this to your pom.xml file
```xml
<dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-sdk</artifactId>
      <version>{opentelemetry.version}</version>
</dependency>
<dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-sdk-trace</artifactId>
      <version>{opentelemetry.version}</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
    <version>{opentelemetry.version}</version>
</dependency>
```
If you are using Gradle, add this to your dependencies
```Groovy
compile 'io.opentelemetry:opentelemetry-sdk:{opentelemetry.version}'
compile 'io.opentelemetry:opentelemetry-sdk-trace:{opentelemetry.version}'
compile 'io.opentelemetry:opentelemetry-exporter-oltp:{opentelemetry.version}'
```
#### OpenTelemetry Configuration

> Note: Enabling OpenTelemetry traces will automatically disable OpenCensus traces.

```java
// Enable OpenTelemetry traces
SpannerOptions.enableOpenTelemetryTraces();

// Create a new tracer provider
SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
      // Use Otlp exporter or any other exporter of your choice.
      .addSpanProcessor(SimpleSpanProcessor.builder(OtlpGrpcSpanExporter
          .builder().build()).build())
          .build();


OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .build()

SpannerOptions options = SpannerOptions.newBuilder()
// Inject OpenTelemetry object via Spanner Options or register OpenTelmetry object as Global
  .setOpenTelemetry(openTelemetry)
  .build();

Spanner spanner = options.getService();
```

## Migrate from OpenCensus to OpenTelemetry

> Using the [OpenTelemetry OpenCensus Bridge](https://mvnrepository.com/artifact/io.opentelemetry/opentelemetry-opencensus-shim), you can immediately begin exporting your metrics and traces with OpenTelemetry

#### Disable OpenCensus metrics
Disable OpenCensus metrics for Spanner by including the following code if you still possess OpenCensus dependencies and exporter.

```java
SpannerOptions.disableOpenCensusMetrics();
```

#### Disable OpenCensus traces
Enabling OpenTelemetry traces for Spanner will automatically disable OpenCensus traces.

```java
SpannerOptions.enableOpenTelemetryTraces();
```

#### Remove OpenCensus Dependencies and Code
Remove any OpenCensus-related code and dependencies from your codebase if all your dependencies are ready to move to OpenTelemetry.

* Remove the OpenCensus Exporters which were configured [here](#configure-the-opencensus-exporter)
* Remove SpannerRPCViews reference which were configured [here](#enable-rpc-views)
* Remove the OpenCensus dependencies which were added [here](#opencensus-dependencies)

#### Update your Dashboards and Alerts

Update your dashboards and alerts to reflect below changes
* **Metrics name** : `cloud.google.com/java` prefix has been removed from OpenTelemery metrics and instead has been added as Instrumenation Scope.
* **Metrics namespace** : OpenTelmetry exporters uses `workload.googleapis.com` namespace opposed to `custom.googleapis.com` with OpenCensus. 



## Samples

Samples are in the [`samples/`](https://github.com/googleapis/java-spanner/tree/main/samples) directory.

| Sample                      | Source Code                       | Try it |
| --------------------------- | --------------------------------- | ------ |
| Database Operations | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/native-image/src/main/java/com/example/spanner/DatabaseOperations.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/native-image/src/main/java/com/example/spanner/DatabaseOperations.java) |
| Instance Operations | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/native-image/src/main/java/com/example/spanner/InstanceOperations.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/native-image/src/main/java/com/example/spanner/InstanceOperations.java) |
| Native Image Spanner Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/native-image/src/main/java/com/example/spanner/NativeImageSpannerSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/native-image/src/main/java/com/example/spanner/NativeImageSpannerSample.java) |
| Add And Drop Database Role | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AddAndDropDatabaseRole.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AddAndDropDatabaseRole.java) |
| Add Json Column Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AddJsonColumnSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AddJsonColumnSample.java) |
| Add Jsonb Column Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AddJsonbColumnSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AddJsonbColumnSample.java) |
| Add Numeric Column Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AddNumericColumnSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AddNumericColumnSample.java) |
| Add Proto Column Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AddProtoColumnSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AddProtoColumnSample.java) |
| Alter Sequence Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AlterSequenceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AlterSequenceSample.java) |
| Alter Table With Foreign Key Delete Cascade Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AlterTableWithForeignKeyDeleteCascadeSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AlterTableWithForeignKeyDeleteCascadeSample.java) |
| Async Dml Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AsyncDmlExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AsyncDmlExample.java) |
| Async Query Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AsyncQueryExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AsyncQueryExample.java) |
| Async Query To List Async Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AsyncQueryToListAsyncExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AsyncQueryToListAsyncExample.java) |
| Async Read Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AsyncReadExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AsyncReadExample.java) |
| Async Read Only Transaction Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AsyncReadOnlyTransactionExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AsyncReadOnlyTransactionExample.java) |
| Async Read Row Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AsyncReadRowExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AsyncReadRowExample.java) |
| Async Read Using Index Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AsyncReadUsingIndexExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AsyncReadUsingIndexExample.java) |
| Async Runner Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AsyncRunnerExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AsyncRunnerExample.java) |
| Async Transaction Manager Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/AsyncTransactionManagerExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/AsyncTransactionManagerExample.java) |
| Batch Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/BatchSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/BatchSample.java) |
| Batch Write At Least Once Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/BatchWriteAtLeastOnceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/BatchWriteAtLeastOnceSample.java) |
| Copy Backup Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CopyBackupSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CopyBackupSample.java) |
| Create Backup With Encryption Key | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateBackupWithEncryptionKey.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateBackupWithEncryptionKey.java) |
| Create Database With Default Leader Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateDatabaseWithDefaultLeaderSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateDatabaseWithDefaultLeaderSample.java) |
| Create Database With Encryption Key | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateDatabaseWithEncryptionKey.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateDatabaseWithEncryptionKey.java) |
| Create Database With Version Retention Period Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateDatabaseWithVersionRetentionPeriodSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateDatabaseWithVersionRetentionPeriodSample.java) |
| Create Instance Config Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateInstanceConfigSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateInstanceConfigSample.java) |
| Create Instance Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateInstanceExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateInstanceExample.java) |
| Create Instance With Autoscaling Config Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateInstanceWithAutoscalingConfigExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateInstanceWithAutoscalingConfigExample.java) |
| Create Instance With Processing Units Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateInstanceWithProcessingUnitsExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateInstanceWithProcessingUnitsExample.java) |
| Create Sequence Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateSequenceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateSequenceSample.java) |
| Create Table With Foreign Key Delete Cascade Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateTableWithForeignKeyDeleteCascadeSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateTableWithForeignKeyDeleteCascadeSample.java) |
| Custom Timeout And Retry Settings Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CustomTimeoutAndRetrySettingsExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CustomTimeoutAndRetrySettingsExample.java) |
| Delete Instance Config Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/DeleteInstanceConfigSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/DeleteInstanceConfigSample.java) |
| Delete Using Dml Returning Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/DeleteUsingDmlReturningSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/DeleteUsingDmlReturningSample.java) |
| Directed Read Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/DirectedReadSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/DirectedReadSample.java) |
| Drop Foreign Key Constraint Delete Cascade Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/DropForeignKeyConstraintDeleteCascadeSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/DropForeignKeyConstraintDeleteCascadeSample.java) |
| Drop Sequence Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/DropSequenceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/DropSequenceSample.java) |
| Enable Fine Grained Access | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/EnableFineGrainedAccess.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/EnableFineGrainedAccess.java) |
| Get Commit Stats Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/GetCommitStatsSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/GetCommitStatsSample.java) |
| Get Database Ddl Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/GetDatabaseDdlSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/GetDatabaseDdlSample.java) |
| Get Instance Config Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/GetInstanceConfigSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/GetInstanceConfigSample.java) |
| Insert Using Dml Returning Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/InsertUsingDmlReturningSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/InsertUsingDmlReturningSample.java) |
| List Database Roles | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/ListDatabaseRoles.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/ListDatabaseRoles.java) |
| List Databases Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/ListDatabasesSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/ListDatabasesSample.java) |
| List Instance Config Operations Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/ListInstanceConfigOperationsSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/ListInstanceConfigOperationsSample.java) |
| List Instance Configs Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/ListInstanceConfigsSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/ListInstanceConfigsSample.java) |
| Pg Alter Sequence Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgAlterSequenceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgAlterSequenceSample.java) |
| Pg Async Query To List Async Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgAsyncQueryToListAsyncExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgAsyncQueryToListAsyncExample.java) |
| Pg Async Runner Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgAsyncRunnerExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgAsyncRunnerExample.java) |
| Pg Async Transaction Manager Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgAsyncTransactionManagerExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgAsyncTransactionManagerExample.java) |
| Pg Batch Dml Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgBatchDmlSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgBatchDmlSample.java) |
| Pg Case Sensitivity Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgCaseSensitivitySample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgCaseSensitivitySample.java) |
| Pg Create Sequence Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgCreateSequenceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgCreateSequenceSample.java) |
| Pg Delete Using Dml Returning Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgDeleteUsingDmlReturningSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgDeleteUsingDmlReturningSample.java) |
| Pg Drop Sequence Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgDropSequenceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgDropSequenceSample.java) |
| Pg Insert Using Dml Returning Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgInsertUsingDmlReturningSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgInsertUsingDmlReturningSample.java) |
| Pg Interleaved Table Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgInterleavedTableSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgInterleavedTableSample.java) |
| Pg Partitioned Dml Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgPartitionedDmlSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgPartitionedDmlSample.java) |
| Pg Query With Numeric Parameter Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgQueryWithNumericParameterSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgQueryWithNumericParameterSample.java) |
| Pg Spanner Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgSpannerSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgSpannerSample.java) |
| Pg Update Using Dml Returning Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgUpdateUsingDmlReturningSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgUpdateUsingDmlReturningSample.java) |
| Query Information Schema Database Options Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/QueryInformationSchemaDatabaseOptionsSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/QueryInformationSchemaDatabaseOptionsSample.java) |
| Query With Json Parameter Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/QueryWithJsonParameterSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/QueryWithJsonParameterSample.java) |
| Query With Jsonb Parameter Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/QueryWithJsonbParameterSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/QueryWithJsonbParameterSample.java) |
| Query With Numeric Parameter Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/QueryWithNumericParameterSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/QueryWithNumericParameterSample.java) |
| Query With Proto Parameter Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/QueryWithProtoParameterSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/QueryWithProtoParameterSample.java) |
| Quickstart Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/QuickstartSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/QuickstartSample.java) |
| Read Data With Database Role | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/ReadDataWithDatabaseRole.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/ReadDataWithDatabaseRole.java) |
| Restore Backup With Encryption Key | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/RestoreBackupWithEncryptionKey.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/RestoreBackupWithEncryptionKey.java) |
| Set Max Commit Delay Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/SetMaxCommitDelaySample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/SetMaxCommitDelaySample.java) |
| Singer Proto | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/SingerProto.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/SingerProto.java) |
| Spanner Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/SpannerSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/SpannerSample.java) |
| Statement Timeout Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/StatementTimeoutExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/StatementTimeoutExample.java) |
| Tag Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/TagSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/TagSample.java) |
| Tracing Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/TracingSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/TracingSample.java) |
| Transaction Timeout Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/TransactionTimeoutExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/TransactionTimeoutExample.java) |
| Update Database Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateDatabaseSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateDatabaseSample.java) |
| Update Database With Default Leader Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateDatabaseWithDefaultLeaderSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateDatabaseWithDefaultLeaderSample.java) |
| Update Instance Config Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateInstanceConfigSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateInstanceConfigSample.java) |
| Update Json Data Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateJsonDataSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateJsonDataSample.java) |
| Update Jsonb Data Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateJsonbDataSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateJsonbDataSample.java) |
| Update Numeric Data Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateNumericDataSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateNumericDataSample.java) |
| Update Proto Data Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateProtoDataSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateProtoDataSample.java) |
| Update Proto Data Sample Using Dml | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateProtoDataSampleUsingDml.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateProtoDataSampleUsingDml.java) |
| Update Using Dml Returning Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateUsingDmlReturningSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateUsingDmlReturningSample.java) |
| Add And Drop Database Role | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/AddAndDropDatabaseRole.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/AddAndDropDatabaseRole.java) |
| Add Json Column Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/AddJsonColumnSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/AddJsonColumnSample.java) |
| Add Jsonb Column Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/AddJsonbColumnSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/AddJsonbColumnSample.java) |
| Add Numeric Column Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/AddNumericColumnSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/AddNumericColumnSample.java) |
| Alter Sequence Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/AlterSequenceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/AlterSequenceSample.java) |
| Alter Table With Foreign Key Delete Cascade Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/AlterTableWithForeignKeyDeleteCascadeSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/AlterTableWithForeignKeyDeleteCascadeSample.java) |
| Copy Backup Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/CopyBackupSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/CopyBackupSample.java) |
| Create Backup With Encryption Key | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateBackupWithEncryptionKey.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateBackupWithEncryptionKey.java) |
| Create Database With Default Leader Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateDatabaseWithDefaultLeaderSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateDatabaseWithDefaultLeaderSample.java) |
| Create Database With Encryption Key | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateDatabaseWithEncryptionKey.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateDatabaseWithEncryptionKey.java) |
| Create Database With Version Retention Period Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateDatabaseWithVersionRetentionPeriodSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateDatabaseWithVersionRetentionPeriodSample.java) |
| Create Instance Config Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateInstanceConfigSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateInstanceConfigSample.java) |
| Create Instance Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateInstanceExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateInstanceExample.java) |
| Create Instance With Autoscaling Config Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateInstanceWithAutoscalingConfigExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateInstanceWithAutoscalingConfigExample.java) |
| Create Instance With Processing Units Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateInstanceWithProcessingUnitsExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateInstanceWithProcessingUnitsExample.java) |
| Create Sequence Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateSequenceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateSequenceSample.java) |
| Create Table With Foreign Key Delete Cascade Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateTableWithForeignKeyDeleteCascadeSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/CreateTableWithForeignKeyDeleteCascadeSample.java) |
| Delete Instance Config Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/DeleteInstanceConfigSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/DeleteInstanceConfigSample.java) |
| Drop Foreign Key Constraint Delete Cascade Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/DropForeignKeyConstraintDeleteCascadeSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/DropForeignKeyConstraintDeleteCascadeSample.java) |
| Drop Sequence Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/DropSequenceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/DropSequenceSample.java) |
| Enable Fine Grained Access | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/EnableFineGrainedAccess.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/EnableFineGrainedAccess.java) |
| Get Database Ddl Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/GetDatabaseDdlSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/GetDatabaseDdlSample.java) |
| Get Instance Config Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/GetInstanceConfigSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/GetInstanceConfigSample.java) |
| List Database Roles | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/ListDatabaseRoles.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/ListDatabaseRoles.java) |
| List Databases Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/ListDatabasesSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/ListDatabasesSample.java) |
| List Instance Config Operations Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/ListInstanceConfigOperationsSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/ListInstanceConfigOperationsSample.java) |
| List Instance Configs Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/ListInstanceConfigsSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/ListInstanceConfigsSample.java) |
| Pg Alter Sequence Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/PgAlterSequenceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/PgAlterSequenceSample.java) |
| Pg Case Sensitivity Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/PgCaseSensitivitySample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/PgCaseSensitivitySample.java) |
| Pg Create Sequence Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/PgCreateSequenceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/PgCreateSequenceSample.java) |
| Pg Drop Sequence Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/PgDropSequenceSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/PgDropSequenceSample.java) |
| Pg Interleaved Table Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/PgInterleavedTableSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/PgInterleavedTableSample.java) |
| Pg Spanner Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/PgSpannerSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/PgSpannerSample.java) |
| Restore Backup With Encryption Key | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/RestoreBackupWithEncryptionKey.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/RestoreBackupWithEncryptionKey.java) |
| Spanner Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/SpannerSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/SpannerSample.java) |
| Update Database Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/UpdateDatabaseSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/UpdateDatabaseSample.java) |
| Update Database With Default Leader Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/UpdateDatabaseWithDefaultLeaderSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/UpdateDatabaseWithDefaultLeaderSample.java) |
| Update Instance Config Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/admin/archived/UpdateInstanceConfigSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/admin/archived/UpdateInstanceConfigSample.java) |



## Troubleshooting

To get help, follow the instructions in the [shared Troubleshooting document][troubleshooting].

## Transport

Cloud Spanner uses gRPC for the transport layer.

## Supported Java Versions

Java 8 or above is required for using this client.

Google's Java client libraries,
[Google Cloud Client Libraries][cloudlibs]
and
[Google Cloud API Libraries][apilibs],
follow the
[Oracle Java SE support roadmap][oracle]
(see the Oracle Java SE Product Releases section).

### For new development

In general, new feature development occurs with support for the lowest Java
LTS version covered by  Oracle's Premier Support (which typically lasts 5 years
from initial General Availability). If the minimum required JVM for a given
library is changed, it is accompanied by a [semver][semver] major release.

Java 11 and (in September 2021) Java 17 are the best choices for new
development.

### Keeping production systems current

Google tests its client libraries with all current LTS versions covered by
Oracle's Extended Support (which typically lasts 8 years from initial
General Availability).

#### Legacy support

Google's client libraries support legacy versions of Java runtimes with long
term stable libraries that don't receive feature updates on a best efforts basis
as it may not be possible to backport all patches.

Google provides updates on a best efforts basis to apps that continue to use
Java 7, though apps might need to upgrade to current versions of the library
that supports their JVM.

#### Where to find specific information

The latest versions and the supported Java versions are identified on
the individual GitHub repository `github.com/GoogleAPIs/java-SERVICENAME`
and on [google-cloud-java][g-c-j].

## Versioning


This library follows [Semantic Versioning](http://semver.org/).



## Contributing


Contributions to this library are always welcome and highly encouraged.

See [CONTRIBUTING][contributing] for more information how to get started.

Please note that this project is released with a Contributor Code of Conduct. By participating in
this project you agree to abide by its terms. See [Code of Conduct][code-of-conduct] for more
information.


## License

Apache 2.0 - See [LICENSE][license] for more information.

## CI Status

Java Version | Status
------------ | ------
Java 8 | [![Kokoro CI][kokoro-badge-image-2]][kokoro-badge-link-2]
Java 8 OSX | [![Kokoro CI][kokoro-badge-image-3]][kokoro-badge-link-3]
Java 8 Windows | [![Kokoro CI][kokoro-badge-image-4]][kokoro-badge-link-4]
Java 11 | [![Kokoro CI][kokoro-badge-image-5]][kokoro-badge-link-5]

Java is a registered trademark of Oracle and/or its affiliates.

[product-docs]: https://cloud.google.com/spanner/docs/
[javadocs]: https://cloud.google.com/java/docs/reference/google-cloud-spanner/latest/history
[kokoro-badge-image-1]: http://storage.googleapis.com/cloud-devrel-public/java/badges/java-spanner/java7.svg
[kokoro-badge-link-1]: http://storage.googleapis.com/cloud-devrel-public/java/badges/java-spanner/java7.html
[kokoro-badge-image-2]: http://storage.googleapis.com/cloud-devrel-public/java/badges/java-spanner/java8.svg
[kokoro-badge-link-2]: http://storage.googleapis.com/cloud-devrel-public/java/badges/java-spanner/java8.html
[kokoro-badge-image-3]: http://storage.googleapis.com/cloud-devrel-public/java/badges/java-spanner/java8-osx.svg
[kokoro-badge-link-3]: http://storage.googleapis.com/cloud-devrel-public/java/badges/java-spanner/java8-osx.html
[kokoro-badge-image-4]: http://storage.googleapis.com/cloud-devrel-public/java/badges/java-spanner/java8-win.svg
[kokoro-badge-link-4]: http://storage.googleapis.com/cloud-devrel-public/java/badges/java-spanner/java8-win.html
[kokoro-badge-image-5]: http://storage.googleapis.com/cloud-devrel-public/java/badges/java-spanner/java11.svg
[kokoro-badge-link-5]: http://storage.googleapis.com/cloud-devrel-public/java/badges/java-spanner/java11.html
[stability-image]: https://img.shields.io/badge/stability-stable-green
[maven-version-image]: https://img.shields.io/maven-central/v/com.google.cloud/google-cloud-spanner.svg
[maven-version-link]: https://central.sonatype.com/artifact/com.google.cloud/google-cloud-spanner/6.68.1
[authentication]: https://github.com/googleapis/google-cloud-java#authentication
[auth-scopes]: https://developers.google.com/identity/protocols/oauth2/scopes
[predefined-iam-roles]: https://cloud.google.com/iam/docs/understanding-roles#predefined_roles
[iam-policy]: https://cloud.google.com/iam/docs/overview#cloud-iam-policy
[developer-console]: https://console.developers.google.com/
[create-project]: https://cloud.google.com/resource-manager/docs/creating-managing-projects
[cloud-cli]: https://cloud.google.com/cli
[troubleshooting]: https://github.com/googleapis/google-cloud-java/blob/main/TROUBLESHOOTING.md
[contributing]: https://github.com/googleapis/java-spanner/blob/main/CONTRIBUTING.md
[code-of-conduct]: https://github.com/googleapis/java-spanner/blob/main/CODE_OF_CONDUCT.md#contributor-code-of-conduct
[license]: https://github.com/googleapis/java-spanner/blob/main/LICENSE
[enable-billing]: https://cloud.google.com/apis/docs/getting-started#enabling_billing
[enable-api]: https://console.cloud.google.com/flows/enableapi?apiid=spanner.googleapis.com
[libraries-bom]: https://github.com/GoogleCloudPlatform/cloud-opensource-java/wiki/The-Google-Cloud-Platform-Libraries-BOM
[shell_img]: https://gstatic.com/cloudssh/images/open-btn.png

[semver]: https://semver.org/
[cloudlibs]: https://cloud.google.com/apis/docs/client-libraries-explained
[apilibs]: https://cloud.google.com/apis/docs/client-libraries-explained#google_api_client_libraries
[oracle]: https://www.oracle.com/java/technologies/java-se-support-roadmap.html
[g-c-j]: http://github.com/googleapis/google-cloud-java
