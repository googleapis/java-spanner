# Cloud Spanner Client Library for Java

This version includes a benchmarking utility class to wait for the session pool to be ready.

## Usage

### Client Library

To use this version of the client library, run the following commands to install a local package.

```
mvn clean install -DskipITs -Dmaven.test.skip=true -Dcheckstyle.skip -Dclirr.skip -pl com.google.cloud:google-cloud-spanner -am
```

This will build version `1.61.0-SNAPSHOT-benchmark` which you can then specify as the version of google-cloud-spanner in your `pom.xml` file.

### Code Sample

To use the new functionality, you can do the following:

```java
String projectId = "my_project_id";
String instanceId = "my_instance_id";
String databaseId = "my_database_id";

final SpannerOptions options = SpannerOptions.newBuilder().build();
final Spanner spanner = options.getService();
final DatabaseId id = DatabaseId.of(projectId, instanceId, databaseId);
final DatabaseClient databaseClient = spanner.getDatabaseClient(id);

// Blocks until session pool is ready
BenchmarkUtils.waitForSessionPoolToBeReady(databaseClient, options.getSessionPoolOptions());
// Code after here, can assume sessions have already been prepared.
```

### Pre-built Jars

We have also provided pre-built jars for google-cloud-spanner and samples. You can find these at `pre-built-jars`.
