# Session Pool and Channel Pool Configuration

__NOTE__: This document contains detailed background information on how the Cloud Spanner client library
works internally. This document is intended for expert users who want a more in-depth understanding
of how the library works. __It is not necessary to read or understand this document in order to use
the client library__.

## Introduction
The Cloud Spanner Java client uses gRPC for communication with Cloud Spanner. gRPC uses 'channels',
which are roughly equivalent to TCP connections. gRPC channels are handled by the core gRPC library.
The gRPC library creates and maintains channel pools that are used for communication with Cloud Spanner.
The client application provides configuration for how those channels should be created, how many
should be created etc. This document describes the default configuration that the Cloud Spanner Java
client uses for channels and sessions, and what configuration options can be tuned for applications
that require a higher throughput than is possible with the default configuration.


### Sessions
Cloud Spanner uses sessions for all data operations. A session belongs to a specific database.
Executing a query or starting a transaction will use a session. One session can only execute one
transaction at any time. This means that an application will need as many sessions as the number of
concurrent transactions that the application will execute. Note that executing a query without a
transaction on Cloud Spanner will automatically cause Cloud Spanner to use a single-use read-only
transaction for the query. The session pool keeps track of the metric `MAX_IN_USE_SESSIONS` to help
users understand how many sessions are used by the application.

Admin operations such as creating instances and databases, but also all DDL statements, do not
require a session.


### gRPC Channels
gRPC channels are used by the Cloud Spanner client for communication. One gRPC channel is roughly
equivalent to a TCP connection. One gRPC channel can handle up to 100 concurrent requests. This
means that an application will need at least as many gRPC channels as the number of concurrent
requests the application will execute, divided by 100.

Each Spanner client creates a new channel pool. Each `DatabaseClient` that is created by the same
`Spanner` client will use the same channel pool.

The Cloud Spanner client will affiliate each session with one channel. This ensures that the same
channel is used for each request that uses the same session, which improves affinity to backend servers.


### Session Pool and Channel Pool
The Java client creates and maintains a session pool for each `DatabaseClient` that is created by the
client. The session pool configuration can be customized by the client application.

The relationship between channel pools and session pools is:
1. A client application creates a [Spanner](https://github.com/googleapis/java-spanner/blob/-/google-cloud-spanner/src/main/java/com/google/cloud/spanner/Spanner.java)
   client. A `Spanner` client is specific to a Google Cloud project and holds the credentials that
   should be used to connect to Cloud Spanner. A `Spanner` client can create one or more
   [DatabaseClients](https://github.com/googleapis/java-spanner/blob/-/google-cloud-spanner/src/main/java/com/google/cloud/spanner/DatabaseClient.java)
   for data operations, and [DatabaseAdminClients](https://github.com/googleapis/java-spanner/blob/-/google-cloud-spanner/src/main/java/com/google/cloud/spanner/DatabaseAdminClient.java)
   and [InstanceAdminClients](https://github.com/googleapis/java-spanner/blob/-/google-cloud-spanner/src/main/java/com/google/cloud/spanner/InstanceAdminClient.java)
   for admin operations.
1. A `Spanner` client creates [one channel pool](https://github.com/googleapis/java-spanner/blob/ee72615b7cbf5c1d6241611acf38b50426bccac1/google-cloud-spanner/src/main/java/com/google/cloud/spanner/spi/v1/GapicSpannerRpc.java#L319)
   that will be shared by all `DatabaseClients` that are created by this `Spanner` client.
1. `DatabaseClients` are specific to a single database. A `DatabaseClient` contains the [session pool](https://github.com/googleapis/java-spanner/blob/-/google-cloud-spanner/src/main/java/com/google/cloud/spanner/SessionPool.java)
   that is specific for that database.
1. Multiple session pools (`DatabaseClients`) that are created by the same `Spanner` client will share
   the same channel pool.


## Default Configuration and Behavior

### Session and Channel Pool

The Java `Spanner` client will by default:
1. Create a gRPC channel pool of fixed size with 4 channels (configurable).
1. Each `DatabaseClient` that is created by a `Spanner` client will create a session pool with
   `MinSessions=100` and `MaxSessions=400`. These values are configurable.
1. The initial 100 sessions that are created by the pool are evenly distributed over the channels.
   This means that the pool will execute 4 (varies with the number of channels that have been configured)
   `BatchCreateSessions` RPCs, each using a separate channel. A channel pool hint is added to all of
   the sessions that are created to ensure that all subsequent RPCs on those sessions use the same
   channel. This behavior is fixed and cannot be changed through configuration.
1. The sessions that are returned by the initial `BatchCreateSessions` RPCs are added in random order
   to the session pool. This gives a uniform distribution of channels across the session pool. This
   behavior is not configurable.
1. The session pool will dynamically create new sessions if all sessions that are currently in the
   pool have been checked out and a new session is requested by the application. Session creation is
   executed asynchronously using the internal thread pool that is also used for gRPC invocations
   (see below). The thread that requested a session will be blocked until either at least one new
   session has been created, or another thread returns a session to the pool.
1. Growth of the session pool is done in steps of 25 sessions (not configurable). Each step of 25
   sessions is created in a single `BatchCreateSessions` RPC. The channel that is used for this RPC is
   changed round-robin for each new growth RPC. This means that the first ‘growth RPC’ will create
   slight overuse of the first channel, and that it will even out as more growth happens. This
   behavior is not configurable.
1. Sessions are taken from and returned to the session pool in LIFO order. This behavior is fixed
   and not configurable.
1. The session pool does not prepare read/write transactions on the sessions. Instead, the client
   library will inline the `BeginTransaction` option with the first statement in a transaction. This
   behavior is not configurable.

The above values that are marked as configurable can only be changed at creation, and are not
modifiable after a session pool / channel pool has been created.


### gRPC Libraries

The gRPC libraries will:
1. Create the actual channel pool with `NumChannels` (configurable) and maintain this. Dropped
   channels etc. are handled transparently by the gRPC library.
1. Create an internal thread pool that is used for RPC invocations. The thread pool will scale
   dynamically with actual usage. Each RPC invocation is handled by a separate thread, and threads 
   in the pool will be reused. This means that the thread pool will at most contain as many threads
   as there have been parallel RPC invocations. It will automatically scale down the number of
   threads if there are less RPC invocations for a while (scale down happens after 60 seconds).
1. The gRPC thread pool is shared among all gRPC channels created in the same JVM.
1. The above can be configured by setting a custom `ChannelProvider` in the `SpannerOptions`. Using this
    requires in-depth knowledge of the gRPC libraries.


## Tuning Options

### More Pods/VMs vs Larger Session Pools
High throughput systems that need to scale can choose between creating a smaller number of large
pods/VMs for the client application with more memory and CPU and a large session pool, or creating a
larger number of smaller pods/VMs with a smaller session pool.

There’s no fundamental difference in performance between the two choices, as long as the size and
available resources of a single pod/VM is aligned with the size of the session pool and expected QPS
that it will receive, and the total size of the cluster is able to scale to a size that can handle
the expected maximum number of transactions.

Note that:
* The `MaxSessions` setting for a pod is the maximum number of parallel transactions or queries that a
  pod could theoretically execute.
* The number of gRPC channels that is required is the maximum number of parallel requests/100 that a
  pod could theoretically execute. One channel can handle multiple requests from different
  transactions concurrently.
* Setting a high value for `MaxSessions` on a pod with a small number of CPUs, limited memory and/or
  limited amount of network bandwidth therefore does not make sense, as that pod will not have the
  resources available to execute a large number of parallel transactions.
* Setting a high value for `MinSessions` for a pod that does not execute many parallel transactions
  will cause that pod to spend resources to keep those sessions alive.
* A rule of thumb is that having more than 100 sessions per CPU core in a pod/VM is unusual, as it
  would mean that one CPU core is expected to execute more than 100 concurrent transactions.

### Session Pool and Channel Pool Options
__Note: All the below considerations are at the level of a single pod/VM.__

When considering whether you need to increase `MaxSessions` or `NumChannels`, you should always consider
whether one single pod might execute more than a certain number of parallel transactions or queries,
and not the total number of parallel transactions your system might have.

#### Num Channels
The [NumChannels option](https://github.com/googleapis/java-spanner/blob/977b5851b9cc1a538dedf57d006483dc58ec90b9/google-cloud-spanner/src/main/java/com/google/cloud/spanner/SpannerOptions.java#L823)
in the Spanner options determines how many channels the underlying gRPC channel pool will contain.
The default is 4. One gRPC channel can multiplex __at most__ 100 parallel RPC invocations. The default 4
channels align with the default `MaxSessions=400`, as the `MaxSessions` value also determines the maximum
number of parallel queries/transactions that can be executed by a single database client.

NumChannels should be __at least__ `MaxSessions / 100`. E.g. `MaxSessions=1000` => `NumChannels=10`.

Spanner clients that create multiple different session pools (e.g. for multi-tenancy purposes) should
set `NumChannels` to at least the maximum number of concurrent requests/100 that the `Spanner` client
will execute. You should also consider lowering the value for `MinSessions` and `MaxSessions` for these
clients. The value for both should be set to the maximum number of concurrent transactions that one
database client will execute. Keeping a large number of unused sessions alive will unnecessarily
consume resources.

Increasing the number of channels beyond `MaxSessions/100` on high throughput systems can reduce p95/p99
latencies, as the probability of random channel congestion is reduced. Creating more channels means
creating more TCP connections, which will increase memory usage slightly. Creating too many channels
can cause some channels to be used too little. This can cause Google infrastructure to close infrequently
used channels, causing higher latencies as channels more often need to be re-created.

#### MinSessions and MaxSessions
`MinSessions` determines the number of sessions that are always created for a session pool.
The default is 100. This number should be high enough to serve the normal application load for that
pod/database pair. Each transaction, including single-use read-only transactions, requires a session.

`MaxSessions` determines the maximum number of sessions that will ever be created by the pool on that
pod. The default is 400. This number should be high enough to serve the highest number of parallel
transactions that is expected on a single pod/database pair.

It is recommended to set `MinSessions=MaxSessions` on high throughput systems to prevent unnecessary
up- and downscaling of the session pool.

Setting `MinSessions` to a much larger value than the number of transactions that a pod/database pair
will ever execute in parallel will degrade performance. Most of these sessions will not be used, and
the client library will have to actively keep these alive. This will cost additional resources both
on the client and the server.

## Life of a Request
An application that uses the Cloud Spanner Java client library will execute queries and transactions
using the public API of the client library. This section explains what happens internally in the
client library during such a request.

### Life of a Single Query
Executing a single query or read operation using the Java client library uses a
'single use read context' like this:

```java
DatabaseClient client =
    spanner.getDatabaseClient(DatabaseId.of("my-project", "my-instance", "my-database"));
try (ResultSet resultSet =
    client.singleUse().executeQuery(Statement.of("select col1, col2 from my_table"))) {
  while (resultSet.next()) {
    // use the results.
  }
}
```

This will cause the following to happen internally in the client library:
1. The `singleUse()` method call returns a ReadContext. Internally, this method checks out a session
   from the session pool. The call to check out a session from the pool is always non-blocking, and
   if no session is available in the pool, the read context will instead be assigned a reference to
   a future session. Checking out a session from the pool can also initiate an RPC to create more
   sessions in the pool, if all sessions in the pool at that moment are in use. This RPC will be
   executed in the background using the default gRPC thread pool.
1. The `executeQuery(..)` call returns a ResultSet. This call is also non-blocking and will prepare
   the request that is needed to execute the query. The actual RPC invocation to execute the query
   will be delayed until the first call to `ResultSet#next()`.
1. The first call to ResultSet#next() will:
   1. If necessary, block until the session that was checked out in step 1 comes available.
   1. Invoke the `ExecuteStreamingSql` RPC on Cloud Spanner. The RPC invocation will use a thread
      from the default gRPC thread pool. The stream that is returned by this RPC will fill the result
      set with data. 
   1. Subsequent calls to `ResultSet#next()` will return data row-by-row that is yielded by the
      stream that was returned in step 3.ii.
1. Closing the result set will return the session that was checked out in step 1 to the pool. It is
   therefore good practice to use all result sets in a try-with-resources block to ensure it is always
   closed. Failing to close a result set will cause a session leak. The result set is also automatically
   closed when all data has been consumed.

### Life of a Read/Write Transaction
Executing a read/write transaction using the Java client library uses a TransactionRunner like this:

```java
client
    .readWriteTransaction()
    .run(
        transaction -> {
          try (ResultSet resultSet =
              transaction.executeQuery(Statement.of("select col1, col2 from my_table"))) {
            while (resultSet.next()) {
              // use the results.
            }
          }
          return transaction.executeUpdate(
              Statement.newBuilder("update my_table set col2=@value where col1=@id")
                  .bind("value")
                  .to("new-value")
                  .bind("id")
                  .to(1L)
                  .build());
        });
```

This will cause the following to happen internally in the client library:
1. The `readWriteTransaction()` method call returns a `TransactionRunner`. Internally, this method checks
   out a session from the session pool. The call to check out a session from the pool is always
   non-blocking, and if no session is available in the pool, the transaction runner will instead be
   assigned a reference to a future session. Checking out a session from the pool can also initiate
   an RPC to create more sessions in the pool, if all sessions in the pool at that moment are in use.
   This RPC will be executed in the background using the default gRPC thread pool.
1. The `TransactionRunner#run(..)` method will execute the given user code in a read/write transaction.
   The user function that is passed to the `TransactionRunner` will receive a reference to a
   `TransactionContext`. All statements in the transaction should use this `TransactionContext`.
1. The client library will not invoke the `BeginTransaction` RPC on Cloud Spanner. Instead, the
   client library will include a `BeginTransaction` option with the first statement that is executed
   using the `TransactionContext`. In the above example, that is the `executeQuery` call. All
   subsequent statements in the same transaction will use the transaction identifier that was
   returned by the first statement.
1. The `executeQuery(..)` call returns a `ResultSet`. This call is also non-blocking and will prepare
   the request that is needed to execute the query. The actual RPC invocation to execute the query
   will be delayed until the first call to `ResultSet#next()`.
1. The first call to `ResultSet#next()` will:
   1. If necessary, block until the session that was checked out in step 1 comes available.
   1. Invoke the `ExecuteStreamingSql` RPC on Cloud Spanner. This RPC will include the `BeginTransaction`
      option. The RPC invocation will use a thread from the default gRPC thread pool. The stream
      that is returned by this RPC will fill the result set with data.
   1. Subsequent calls to `ResultSet#next()` will return data row-by-row that is yielded by the
      stream that was returned in step 5.ii.
1. The `executeUpdate()` call returns an update count. This call is blocking and will directly
   execute the update statement using the `ExecuteSql` RPC. The RPC invocation uses a thread from
   the default gRPC thread pool.
1. The `TransactionRunner` will automatically commit the transaction if the supplied user code
   finished without any errors. The `Commit` RPC that is invoked uses a thread from the default gRPC
   thread pool.
