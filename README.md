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
      <version>26.9.0</version>
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

If you are using Maven without BOM, add this to your dependencies:


```xml
<dependency>
  <groupId>com.google.cloud</groupId>
  <artifactId>google-cloud-spanner</artifactId>
  <version>6.36.1</version>
</dependency>

```

If you are using Gradle 5.x or later, add this to your dependencies:

```Groovy
implementation platform('com.google.cloud:libraries-bom:26.9.0')

implementation 'com.google.cloud:google-cloud-spanner'
```
If you are using Gradle without BOM, add this to your dependencies:

```Groovy
implementation 'com.google.cloud:google-cloud-spanner:6.36.1'
```

If you are using SBT, add this to your dependencies:

```Scala
libraryDependencies += "com.google.cloud" % "google-cloud-spanner" % "6.36.1"
```

## Authentication

See the [Authentication][authentication] section in the base directory's README.

## Authorization

The client application making API calls must be granted [authorization scopes][auth-scopes] required for the desired Cloud Spanner APIs, and the authenticated principal must have the [IAM role(s)][predefined-iam-roles] required to access GCP resources using the Cloud Spanner API calls.

## Getting Started

### Prerequisites

You will need a [Google Cloud Platform Console][developer-console] project with the Cloud Spanner [API enabled][enable-api].
You will need to [enable billing][enable-billing] to use Google Cloud Spanner.
[Follow these instructions][create-project] to get your project set up. You will also need to set up the local development environment by
[installing the Google Cloud SDK][cloud-sdk] and running the following commands in command line:
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

## OpenCensus Metrics

Cloud Spanner client supports [Opencensus Metrics](https://opencensus.io/stats/),
which gives insight into the client internals and aids in debugging/troubleshooting
production issues. OpenCensus metrics will provide you with enough data to enable you to
spot, and investigate the cause of any unusual deviations from normal behavior.

All Cloud Spanner Metrics are prefixed with `cloud.google.com/java/spanner/`. The
metrics will be tagged with:
* `database`: the target database name.
* `instance_id`: the instance id of the target Spanner instance.
* `client_id`: the user defined database client id.
* `library_version`: the version of the library that you're using.

> Note: RPC level metrics can be gleaned from gRPC’s metrics, which are prefixed
with `grpc.io/client/`.
### Available client-side metrics:

* `cloud.google.com/java/spanner/max_in_use_sessions`: This returns the maximum
  number of sessions that have been in use during the last maintenance window
  interval, so as to provide an indication of the amount of activity currently
  in the database.

* `cloud.google.com/java/spanner/max_allowed_sessions`: This shows the maximum
  number of sessions allowed.

* `cloud.google.com/java/spanner/num_sessions_in_pool`: This metric allows users to
   see instance-level and database-level data for the total number of sessions in
   the pool at this very moment.

* `cloud.google.com/java/spanner/num_acquired_sessions`: This metric allows
  users to see the total number of acquired sessions.

* `cloud.google.com/java/spanner/num_released_sessions`: This metric allows
  users to see the total number of released (destroyed) sessions.

* `cloud.google.com/java/spanner/get_session_timeouts`: This gives you an
  indication of the total number of get session timed-out instead of being
  granted (the thread that requested the session is placed in a wait queue where
  it waits until a session is released into the pool by another thread) due to
  pool exhaustion since the server process started.

* `cloud.google.com/java/spanner/gfe_latency`: This metric shows latency between
  Google's network receiving an RPC and reading back the first byte of the response.

* `cloud.google.com/java/spanner/gfe_header_missing_count`: This metric shows the
  number of RPC responses received without the server-timing header, most likely
  indicating that the RPC never reached Google's network.

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

By default, the functionality is disabled. You need to include opencensus-impl
dependency to collect the data and exporter dependency to export to backend.

[Click here](https://medium.com/google-cloud/troubleshooting-cloud-spanner-applications-with-opencensus-2cf424c4c590) for more information.




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
| Copy Backup Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CopyBackupSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CopyBackupSample.java) |
| Create Backup With Encryption Key | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateBackupWithEncryptionKey.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateBackupWithEncryptionKey.java) |
| Create Database With Default Leader Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateDatabaseWithDefaultLeaderSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateDatabaseWithDefaultLeaderSample.java) |
| Create Database With Encryption Key | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateDatabaseWithEncryptionKey.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateDatabaseWithEncryptionKey.java) |
| Create Database With Version Retention Period Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateDatabaseWithVersionRetentionPeriodSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateDatabaseWithVersionRetentionPeriodSample.java) |
| Create Instance Config Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateInstanceConfigSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateInstanceConfigSample.java) |
| Create Instance Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateInstanceExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateInstanceExample.java) |
| Create Instance With Processing Units Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CreateInstanceWithProcessingUnitsExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CreateInstanceWithProcessingUnitsExample.java) |
| Custom Timeout And Retry Settings Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/CustomTimeoutAndRetrySettingsExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/CustomTimeoutAndRetrySettingsExample.java) |
| Delete Instance Config Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/DeleteInstanceConfigSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/DeleteInstanceConfigSample.java) |
| Delete Using Dml Returning Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/DeleteUsingDmlReturningSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/DeleteUsingDmlReturningSample.java) |
| Enable Fine Grained Access | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/EnableFineGrainedAccess.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/EnableFineGrainedAccess.java) |
| Get Commit Stats Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/GetCommitStatsSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/GetCommitStatsSample.java) |
| Get Database Ddl Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/GetDatabaseDdlSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/GetDatabaseDdlSample.java) |
| Get Instance Config Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/GetInstanceConfigSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/GetInstanceConfigSample.java) |
| Insert Using Dml Returning Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/InsertUsingDmlReturningSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/InsertUsingDmlReturningSample.java) |
| List Database Roles | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/ListDatabaseRoles.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/ListDatabaseRoles.java) |
| List Databases Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/ListDatabasesSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/ListDatabasesSample.java) |
| List Instance Config Operations Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/ListInstanceConfigOperationsSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/ListInstanceConfigOperationsSample.java) |
| List Instance Configs Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/ListInstanceConfigsSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/ListInstanceConfigsSample.java) |
| Pg Async Query To List Async Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgAsyncQueryToListAsyncExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgAsyncQueryToListAsyncExample.java) |
| Pg Async Runner Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgAsyncRunnerExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgAsyncRunnerExample.java) |
| Pg Async Transaction Manager Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgAsyncTransactionManagerExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgAsyncTransactionManagerExample.java) |
| Pg Batch Dml Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgBatchDmlSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgBatchDmlSample.java) |
| Pg Case Sensitivity Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgCaseSensitivitySample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgCaseSensitivitySample.java) |
| Pg Delete Using Dml Returning Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/PgDeleteUsingDmlReturningSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/PgDeleteUsingDmlReturningSample.java) |
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
| Quickstart Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/QuickstartSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/QuickstartSample.java) |
| Read Data With Database Role | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/ReadDataWithDatabaseRole.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/ReadDataWithDatabaseRole.java) |
| Restore Backup With Encryption Key | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/RestoreBackupWithEncryptionKey.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/RestoreBackupWithEncryptionKey.java) |
| Spanner Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/SpannerSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/SpannerSample.java) |
| Statement Timeout Example | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/StatementTimeoutExample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/StatementTimeoutExample.java) |
| Tag Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/TagSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/TagSample.java) |
| Tracing Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/TracingSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/TracingSample.java) |
| Update Database With Default Leader Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateDatabaseWithDefaultLeaderSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateDatabaseWithDefaultLeaderSample.java) |
| Update Instance Config Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateInstanceConfigSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateInstanceConfigSample.java) |
| Update Json Data Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateJsonDataSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateJsonDataSample.java) |
| Update Jsonb Data Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateJsonbDataSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateJsonbDataSample.java) |
| Update Numeric Data Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateNumericDataSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateNumericDataSample.java) |
| Update Using Dml Returning Sample | [source code](https://github.com/googleapis/java-spanner/blob/main/samples/snippets/src/main/java/com/example/spanner/UpdateUsingDmlReturningSample.java) | [![Open in Cloud Shell][shell_img]](https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/googleapis/java-spanner&page=editor&open_in_editor=samples/snippets/src/main/java/com/example/spanner/UpdateUsingDmlReturningSample.java) |



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
[maven-version-link]: https://search.maven.org/search?q=g:com.google.cloud%20AND%20a:google-cloud-spanner&core=gav
[authentication]: https://github.com/googleapis/google-cloud-java#authentication
[auth-scopes]: https://developers.google.com/identity/protocols/oauth2/scopes
[predefined-iam-roles]: https://cloud.google.com/iam/docs/understanding-roles#predefined_roles
[iam-policy]: https://cloud.google.com/iam/docs/overview#cloud-iam-policy
[developer-console]: https://console.developers.google.com/
[create-project]: https://cloud.google.com/resource-manager/docs/creating-managing-projects
[cloud-sdk]: https://cloud.google.com/sdk/
[troubleshooting]: https://github.com/googleapis/google-cloud-common/blob/main/troubleshooting/readme.md#troubleshooting
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
