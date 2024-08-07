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

package com.google.cloud.spanner;

import com.google.common.base.Preconditions;
import com.google.spanner.v1.DirectedReadOptions;
import com.google.spanner.v1.ReadRequest.OrderBy;
import com.google.spanner.v1.RequestOptions.Priority;
import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;

/** Specifies options for various spanner operations */
public final class Options implements Serializable {
  private static final long serialVersionUID = 8067099123096783941L;

  /**
   * Priority for an RPC invocation. The default priority is {@link #HIGH}. This enum can be used to
   * set a lower priority for a specific RPC invocation.
   */
  public enum RpcPriority {
    LOW(Priority.PRIORITY_LOW),
    MEDIUM(Priority.PRIORITY_MEDIUM),
    HIGH(Priority.PRIORITY_HIGH),
    UNSPECIFIED(Priority.PRIORITY_UNSPECIFIED);

    private final Priority proto;

    RpcPriority(Priority proto) {
      this.proto = Preconditions.checkNotNull(proto);
    }

    public static RpcPriority fromProto(Priority proto) {
      for (RpcPriority e : RpcPriority.values()) {
        if (e.proto.equals(proto)) return e;
      }
      return RpcPriority.UNSPECIFIED;
    }
  }

  /**
   * OrderBy for an RPC invocation. The default orderby is {@link #PRIMARY_KEY}. This enum can be
   * used to control the order in which rows are returned from a read.
   */
  public enum RpcOrderBy {
    PRIMARY_KEY(OrderBy.ORDER_BY_PRIMARY_KEY),
    NO_ORDER(OrderBy.ORDER_BY_NO_ORDER),
    UNSPECIFIED(OrderBy.ORDER_BY_UNSPECIFIED);

    private final OrderBy proto;

    RpcOrderBy(OrderBy proto) {
      this.proto = Preconditions.checkNotNull(proto);
    }

    public static RpcOrderBy fromProto(OrderBy proto) {
      for (RpcOrderBy e : RpcOrderBy.values()) {
        if (e.proto.equals(proto)) return e;
      }
      return RpcOrderBy.UNSPECIFIED;
    }
  }

  /** Marker interface to mark options applicable to both Read and Query operations */
  public interface ReadAndQueryOption extends ReadOption, QueryOption {}

  /** Marker interface to mark options applicable to read operation */
  public interface ReadOption {}

  /** Marker interface to mark options applicable to Read, Query, Update and Write operations */
  public interface ReadQueryUpdateTransactionOption
      extends ReadOption, QueryOption, UpdateOption, TransactionOption {}

  /** Marker interface to mark options applicable to Update and Write operations */
  public interface UpdateTransactionOption extends UpdateOption, TransactionOption {}

  /**
   * Marker interface to mark options applicable to Create, Update and Delete operations in admin
   * API.
   */
  public interface CreateUpdateDeleteAdminApiOption
      extends CreateAdminApiOption, UpdateAdminApiOption, DeleteAdminApiOption {}

  /** Marker interface to mark options applicable to Create operations in admin API. */
  public interface CreateAdminApiOption extends AdminApiOption {}

  /** Marker interface to mark options applicable to Delete operations in admin API. */
  public interface DeleteAdminApiOption extends AdminApiOption {}

  /** Marker interface to mark options applicable to Update operations in admin API. */
  public interface UpdateAdminApiOption extends AdminApiOption {}

  /** Marker interface to mark options applicable to query operation. */
  public interface QueryOption {}

  /** Marker interface to mark options applicable to write operations */
  public interface TransactionOption {}

  /** Marker interface to mark options applicable to update operation. */
  public interface UpdateOption {}

  /** Marker interface to mark options applicable to list operations in admin API. */
  public interface ListOption {}

  /** Marker interface to mark options applicable to operations in admin API. */
  public interface AdminApiOption {}

  /** Specifying this instructs the transaction to request {@link CommitStats} from the backend. */
  public static TransactionOption commitStats() {
    return COMMIT_STATS_OPTION;
  }

  /**
   * Specifying this instructs the transaction to request Optimistic Lock from the backend. In this
   * concurrency mode, operations during the execution phase, i.e., reads and queries, are performed
   * without acquiring locks, and transactional consistency is ensured by running a validation
   * process in the commit phase (when any needed locks are acquired). The validation process
   * succeeds only if there are no conflicting committed transactions (that committed mutations to
   * the read data at a commit timestamp after the read timestamp).
   */
  public static TransactionOption optimisticLock() {
    return OPTIMISTIC_LOCK_OPTION;
  }

  /**
   * Specifying this instructs the transaction to be excluded from being recorded in change streams
   * with the DDL option `allow_txn_exclusion=true`. This does not exclude the transaction from
   * being recorded in the change streams with the DDL option `allow_txn_exclusion` being false or
   * unset.
   */
  public static UpdateTransactionOption excludeTxnFromChangeStreams() {
    return EXCLUDE_TXN_FROM_CHANGE_STREAMS_OPTION;
  }

  /**
   * Specifying this will cause the read to yield at most this many rows. This should be greater
   * than 0.
   */
  public static ReadOption limit(long limit) {
    Preconditions.checkArgument(limit > 0, "Limit should be greater than 0");
    return new LimitOption(limit);
  }

  /** Specifies the order_by to use for the RPC. */
  public static ReadOption orderBy(RpcOrderBy orderBy) {
    return new OrderByOption(orderBy);
  }

  /**
   * Specifying this will allow the client to prefetch up to {@code prefetchChunks} {@code
   * PartialResultSet} chunks for read and query. The data size of each chunk depends on the server
   * implementation but a good rule of thumb is that each chunk will be up to 1 MiB. Larger values
   * reduce the likelihood of blocking while consuming results at the cost of greater memory
   * consumption. {@code prefetchChunks} should be greater than 0. To get good performance choose a
   * value that is large enough to allow buffering of chunks for an entire row. Apart from the
   * buffered chunks, there can be at most one more row buffered in the client.
   */
  public static ReadAndQueryOption prefetchChunks(int prefetchChunks) {
    Preconditions.checkArgument(prefetchChunks > 0, "prefetchChunks should be greater than 0");
    return new FlowControlOption(prefetchChunks);
  }

  public static ReadAndQueryOption bufferRows(int bufferRows) {
    Preconditions.checkArgument(bufferRows > 0, "bufferRows should be greater than 0");
    return new BufferRowsOption(bufferRows);
  }

  /** Specifies the priority to use for the RPC. */
  public static ReadQueryUpdateTransactionOption priority(RpcPriority priority) {
    return new PriorityOption(priority);
  }

  public static TransactionOption maxCommitDelay(Duration maxCommitDelay) {
    Preconditions.checkArgument(!maxCommitDelay.isNegative(), "maxCommitDelay should be positive");
    return new MaxCommitDelayOption(maxCommitDelay);
  }

  /**
   * Specifying this will cause the reads, queries, updates and writes operations statistics
   * collection to be grouped by tag.
   */
  public static ReadQueryUpdateTransactionOption tag(String name) {
    return new TagOption(name);
  }

  /**
   * Specifying this will cause the list operations to fetch at most this many records in a page.
   */
  public static ListOption pageSize(int pageSize) {
    return new PageSizeOption(pageSize);
  }

  /**
   * If this is for PartitionedRead or PartitionedQuery and this field is set to `true`, the request
   * will be executed via Spanner independent compute resources.
   */
  public static DataBoostQueryOption dataBoostEnabled(Boolean dataBoostEnabled) {
    return new DataBoostQueryOption(dataBoostEnabled);
  }

  /**
   * Specifying this will cause the list operation to start fetching the record from this onwards.
   */
  public static ListOption pageToken(String pageToken) {
    return new PageTokenOption(pageToken);
  }

  /**
   * Specifying this will cause the given filter to be applied to the list operation. List
   * operations that support this options are:
   *
   * <ul>
   *   <li>{@link InstanceAdminClient#listInstances}
   * </ul>
   *
   * If this option is passed to any other list operation, it will throw an
   * IllegalArgumentException.
   *
   * @param filter An expression for filtering the results of the request. Filter rules are case
   *     insensitive. Some examples of using filters are:
   *     <ul>
   *       <li>name:* The entity has a name.
   *       <li>name:Howl The entity's name contains "howl".
   *       <li>name:HOWL Equivalent to above.
   *       <li>NAME:howl Equivalent to above.
   *       <li>labels.env:* The entity has the label env.
   *       <li>labels.env:dev The entity has a label env whose value contains "dev".
   *       <li>name:howl labels.env:dev The entity's name contains "howl" and it has the label env
   *           whose value contains "dev".
   *     </ul>
   */
  public static ListOption filter(String filter) {
    return new FilterOption(filter);
  }

  /**
   * Specifying this will help in optimistic concurrency control as a way to help prevent
   * simultaneous deletes of an instance config from overwriting each other. Operations that support
   * this option are:
   *
   * <ul>
   *   <li>{@link InstanceAdminClient#deleteInstanceConfig}
   * </ul>
   */
  public static DeleteAdminApiOption etag(String etag) {
    return new EtagOption(etag);
  }

  /**
   * Specifying this will not actually execute a request, and provide the same response. Operations
   * that support this option are:
   *
   * <ul>
   *   <li>{@link InstanceAdminClient#createInstanceConfig}
   *   <li>{@link InstanceAdminClient#updateInstanceConfig}
   *   <li>{@link InstanceAdminClient#deleteInstanceConfig}
   * </ul>
   */
  public static CreateUpdateDeleteAdminApiOption validateOnly(Boolean validateOnly) {
    return new ValidateOnlyOption(validateOnly);
  }

  /**
   * Option to request DirectedRead for ReadOnlyTransaction and SingleUseTransaction.
   *
   * <p>The DirectedReadOptions can be used to indicate which replicas or regions should be used for
   * non-transactional reads or queries. Not all requests can be sent to non-leader replicas. In
   * particular, some requests such as reads within read-write transactions must be sent to a
   * designated leader replica. These requests ignore DirectedReadOptions.
   */
  public static ReadAndQueryOption directedRead(DirectedReadOptions directedReadOptions) {
    return new DirectedReadOption(directedReadOptions);
  }

  public static ReadAndQueryOption decodeMode(DecodeMode decodeMode) {
    return new DecodeOption(decodeMode);
  }

  /** Option to request {@link CommitStats} for read/write transactions. */
  static final class CommitStatsOption extends InternalOption implements TransactionOption {
    @Override
    void appendToOptions(Options options) {
      options.withCommitStats = true;
    }
  }

  static final CommitStatsOption COMMIT_STATS_OPTION = new CommitStatsOption();

  /** Option to request {@link MaxCommitDelayOption} for read/write transactions. */
  static final class MaxCommitDelayOption extends InternalOption implements TransactionOption {
    final Duration maxCommitDelay;

    MaxCommitDelayOption(Duration maxCommitDelay) {
      this.maxCommitDelay = maxCommitDelay;
    }

    @Override
    void appendToOptions(Options options) {
      options.maxCommitDelay = maxCommitDelay;
    }
  }

  /** Option to request Optimistic Concurrency Control for read/write transactions. */
  static final class OptimisticLockOption extends InternalOption implements TransactionOption {
    @Override
    void appendToOptions(Options options) {
      options.withOptimisticLock = true;
    }
  }

  static final OptimisticLockOption OPTIMISTIC_LOCK_OPTION = new OptimisticLockOption();

  /** Option to request the transaction to be excluded from change streams. */
  static final class ExcludeTxnFromChangeStreamsOption extends InternalOption
      implements UpdateTransactionOption {
    @Override
    void appendToOptions(Options options) {
      options.withExcludeTxnFromChangeStreams = true;
    }
  }

  static final ExcludeTxnFromChangeStreamsOption EXCLUDE_TXN_FROM_CHANGE_STREAMS_OPTION =
      new ExcludeTxnFromChangeStreamsOption();

  /** Option pertaining to flow control. */
  static final class FlowControlOption extends InternalOption implements ReadAndQueryOption {
    final int prefetchChunks;

    FlowControlOption(int prefetchChunks) {
      this.prefetchChunks = prefetchChunks;
    }

    @Override
    void appendToOptions(Options options) {
      options.prefetchChunks = prefetchChunks;
    }
  }

  static final class BufferRowsOption extends InternalOption implements ReadAndQueryOption {
    final int bufferRows;

    BufferRowsOption(int bufferRows) {
      this.bufferRows = bufferRows;
    }

    @Override
    void appendToOptions(Options options) {
      options.bufferRows = bufferRows;
    }
  }

  static final class PriorityOption extends InternalOption
      implements ReadQueryUpdateTransactionOption {
    private final RpcPriority priority;

    PriorityOption(RpcPriority priority) {
      this.priority = priority;
    }

    @Override
    void appendToOptions(Options options) {
      options.priority = priority;
    }
  }

  static final class TagOption extends InternalOption implements ReadQueryUpdateTransactionOption {
    private final String tag;

    TagOption(String tag) {
      this.tag = tag;
    }

    String getTag() {
      return tag;
    }

    @Override
    void appendToOptions(Options options) {
      options.tag = tag;
    }
  }

  static final class EtagOption extends InternalOption implements DeleteAdminApiOption {
    private final String etag;

    EtagOption(String etag) {
      this.etag = etag;
    }

    @Override
    void appendToOptions(Options options) {
      options.etag = etag;
    }
  }

  static final class ValidateOnlyOption extends InternalOption
      implements CreateUpdateDeleteAdminApiOption {
    private final Boolean validateOnly;

    ValidateOnlyOption(Boolean validateOnly) {
      this.validateOnly = validateOnly;
    }

    @Override
    void appendToOptions(Options options) {
      options.validateOnly = validateOnly;
    }
  }

  static final class DirectedReadOption extends InternalOption implements ReadAndQueryOption {
    private final DirectedReadOptions directedReadOptions;

    DirectedReadOption(DirectedReadOptions directedReadOptions) {
      this.directedReadOptions =
          Preconditions.checkNotNull(directedReadOptions, "DirectedReadOptions cannot be null");
      ;
    }

    @Override
    void appendToOptions(Options options) {
      options.directedReadOptions = directedReadOptions;
    }
  }

  static final class DecodeOption extends InternalOption implements ReadAndQueryOption {
    private final DecodeMode decodeMode;

    DecodeOption(DecodeMode decodeMode) {
      this.decodeMode = Preconditions.checkNotNull(decodeMode, "DecodeMode cannot be null");
    }

    @Override
    void appendToOptions(Options options) {
      options.decodeMode = decodeMode;
    }
  }

  private boolean withCommitStats;

  private Duration maxCommitDelay;

  private Long limit;
  private Integer prefetchChunks;
  private Integer bufferRows;
  private Integer pageSize;
  private String pageToken;
  private String filter;
  private RpcPriority priority;
  private String tag;
  private String etag;
  private Boolean validateOnly;
  private Boolean withOptimisticLock;
  private Boolean withExcludeTxnFromChangeStreams;
  private Boolean dataBoostEnabled;
  private DirectedReadOptions directedReadOptions;
  private DecodeMode decodeMode;
  private RpcOrderBy orderBy;

  // Construction is via factory methods below.
  private Options() {}

  boolean withCommitStats() {
    return withCommitStats;
  }

  boolean hasMaxCommitDelay() {
    return maxCommitDelay != null;
  }

  Duration maxCommitDelay() {
    return maxCommitDelay;
  }

  boolean hasLimit() {
    return limit != null;
  }

  long limit() {
    return limit;
  }

  boolean hasPrefetchChunks() {
    return prefetchChunks != null;
  }

  int prefetchChunks() {
    return prefetchChunks;
  }

  boolean hasBufferRows() {
    return bufferRows != null;
  }

  int bufferRows() {
    return bufferRows;
  }

  boolean hasPageSize() {
    return pageSize != null;
  }

  int pageSize() {
    return pageSize;
  }

  boolean hasPageToken() {
    return pageToken != null;
  }

  String pageToken() {
    return pageToken;
  }

  boolean hasFilter() {
    return filter != null;
  }

  String filter() {
    return filter;
  }

  boolean hasPriority() {
    return priority != null;
  }

  Priority priority() {
    return priority == null ? null : priority.proto;
  }

  boolean hasTag() {
    return tag != null;
  }

  String tag() {
    return tag;
  }

  boolean hasEtag() {
    return etag != null;
  }

  String etag() {
    return etag;
  }

  boolean hasValidateOnly() {
    return validateOnly != null;
  }

  Boolean validateOnly() {
    return validateOnly;
  }

  Boolean withOptimisticLock() {
    return withOptimisticLock;
  }

  Boolean withExcludeTxnFromChangeStreams() {
    return withExcludeTxnFromChangeStreams;
  }

  boolean hasDataBoostEnabled() {
    return dataBoostEnabled != null;
  }

  Boolean dataBoostEnabled() {
    return dataBoostEnabled;
  }

  boolean hasDirectedReadOptions() {
    return directedReadOptions != null;
  }

  DirectedReadOptions directedReadOptions() {
    return directedReadOptions;
  }

  boolean hasDecodeMode() {
    return decodeMode != null;
  }

  DecodeMode decodeMode() {
    return decodeMode;
  }

  boolean hasOrderBy() {
    return orderBy != null;
  }

  OrderBy orderBy() {
    return orderBy == null ? null : orderBy.proto;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    if (withCommitStats) {
      b.append("withCommitStats: ").append(withCommitStats).append(' ');
    }
    if (maxCommitDelay != null) {
      b.append("maxCommitDelay: ").append(maxCommitDelay).append(' ');
    }
    if (limit != null) {
      b.append("limit: ").append(limit).append(' ');
    }
    if (prefetchChunks != null) {
      b.append("prefetchChunks: ").append(prefetchChunks).append(' ');
    }
    if (pageSize != null) {
      b.append("pageSize: ").append(pageSize).append(' ');
    }
    if (pageToken != null) {
      b.append("pageToken: ").append(pageToken).append(' ');
    }
    if (filter != null) {
      b.append("filter: ").append(filter).append(' ');
    }
    if (priority != null) {
      b.append("priority: ").append(priority).append(' ');
    }
    if (tag != null) {
      b.append("tag: ").append(tag).append(' ');
    }
    if (etag != null) {
      b.append("etag: ").append(etag).append(' ');
    }
    if (validateOnly != null) {
      b.append("validateOnly: ").append(validateOnly).append(' ');
    }
    if (withOptimisticLock != null) {
      b.append("withOptimisticLock: ").append(withOptimisticLock).append(' ');
    }
    if (withExcludeTxnFromChangeStreams != null) {
      b.append("withExcludeTxnFromChangeStreams: ")
          .append(withExcludeTxnFromChangeStreams)
          .append(' ');
    }
    if (dataBoostEnabled != null) {
      b.append("dataBoostEnabled: ").append(dataBoostEnabled).append(' ');
    }
    if (directedReadOptions != null) {
      b.append("directedReadOptions: ").append(directedReadOptions).append(' ');
    }
    if (decodeMode != null) {
      b.append("decodeMode: ").append(decodeMode).append(' ');
    }
    if (orderBy != null) {
      b.append("orderBy: ").append(orderBy).append(' ');
    }
    return b.toString();
  }

  @Override
  // Since Options mandates checking hasXX() before XX() is called, the equals & hashCode look more
  // complicated than usual.
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Options that = (Options) o;
    return Objects.equals(withCommitStats, that.withCommitStats)
        && Objects.equals(maxCommitDelay, that.maxCommitDelay)
        && (!hasLimit() && !that.hasLimit()
            || hasLimit() && that.hasLimit() && Objects.equals(limit(), that.limit()))
        && (!hasPrefetchChunks() && !that.hasPrefetchChunks()
            || hasPrefetchChunks()
                && that.hasPrefetchChunks()
                && Objects.equals(prefetchChunks(), that.prefetchChunks()))
        && (!hasBufferRows() && !that.hasBufferRows()
            || hasBufferRows()
                && that.hasBufferRows()
                && Objects.equals(bufferRows(), that.bufferRows()))
        && (!hasPageSize() && !that.hasPageSize()
            || hasPageSize() && that.hasPageSize() && Objects.equals(pageSize(), that.pageSize()))
        && Objects.equals(pageToken(), that.pageToken())
        && Objects.equals(filter(), that.filter())
        && Objects.equals(priority(), that.priority())
        && Objects.equals(tag(), that.tag())
        && Objects.equals(etag(), that.etag())
        && Objects.equals(validateOnly(), that.validateOnly())
        && Objects.equals(withOptimisticLock(), that.withOptimisticLock())
        && Objects.equals(withExcludeTxnFromChangeStreams(), that.withExcludeTxnFromChangeStreams())
        && Objects.equals(dataBoostEnabled(), that.dataBoostEnabled())
        && Objects.equals(directedReadOptions(), that.directedReadOptions())
        && Objects.equals(orderBy(), that.orderBy());
  }

  @Override
  public int hashCode() {
    int result = 31;
    if (withCommitStats) {
      result = 31 * result + 1231;
    }
    if (maxCommitDelay != null) {
      result = 31 * result + maxCommitDelay.hashCode();
    }
    if (limit != null) {
      result = 31 * result + limit.hashCode();
    }
    if (prefetchChunks != null) {
      result = 31 * result + prefetchChunks.hashCode();
    }
    if (bufferRows != null) {
      result = 31 * result + bufferRows.hashCode();
    }
    if (pageSize != null) {
      result = 31 * result + pageSize.hashCode();
    }
    if (pageToken != null) {
      result = 31 * result + pageToken.hashCode();
    }
    if (filter != null) {
      result = 31 * result + filter.hashCode();
    }
    if (priority != null) {
      result = 31 * result + priority.hashCode();
    }
    if (tag != null) {
      result = 31 * result + tag.hashCode();
    }
    if (etag != null) {
      result = 31 * result + etag.hashCode();
    }
    if (validateOnly != null) {
      result = 31 * result + validateOnly.hashCode();
    }
    if (withOptimisticLock != null) {
      result = 31 * result + withOptimisticLock.hashCode();
    }
    if (withExcludeTxnFromChangeStreams != null) {
      result = 31 * result + withExcludeTxnFromChangeStreams.hashCode();
    }
    if (dataBoostEnabled != null) {
      result = 31 * result + dataBoostEnabled.hashCode();
    }
    if (directedReadOptions != null) {
      result = 31 * result + directedReadOptions.hashCode();
    }
    if (decodeMode != null) {
      result = 31 * result + decodeMode.hashCode();
    }
    if (orderBy != null) {
      result = 31 * result + orderBy.hashCode();
    }
    return result;
  }

  static Options fromReadOptions(ReadOption... options) {
    Options readOptions = new Options();
    for (ReadOption option : options) {
      if (option instanceof InternalOption) {
        ((InternalOption) option).appendToOptions(readOptions);
      }
    }
    return readOptions;
  }

  static Options fromQueryOptions(QueryOption... options) {
    Options readOptions = new Options();
    for (QueryOption option : options) {
      if (option instanceof InternalOption) {
        ((InternalOption) option).appendToOptions(readOptions);
      }
    }
    return readOptions;
  }

  static Options fromUpdateOptions(UpdateOption... options) {
    Options updateOptions = new Options();
    for (UpdateOption option : options) {
      if (option instanceof InternalOption) {
        ((InternalOption) option).appendToOptions(updateOptions);
      }
    }
    return updateOptions;
  }

  static Options fromTransactionOptions(TransactionOption... options) {
    Options transactionOptions = new Options();
    for (TransactionOption option : options) {
      if (option instanceof InternalOption) {
        ((InternalOption) option).appendToOptions(transactionOptions);
      }
    }
    return transactionOptions;
  }

  static Options fromListOptions(ListOption... options) {
    Options listOptions = new Options();
    for (ListOption option : options) {
      if (option instanceof InternalOption) {
        ((InternalOption) option).appendToOptions(listOptions);
      }
    }
    return listOptions;
  }

  static Options fromAdminApiOptions(AdminApiOption... options) {
    Options adminApiOptions = new Options();
    for (AdminApiOption option : options) {
      if (option instanceof InternalOption) {
        ((InternalOption) option).appendToOptions(adminApiOptions);
      }
    }
    return adminApiOptions;
  }

  private abstract static class InternalOption {
    abstract void appendToOptions(Options options);
  }

  static class LimitOption extends InternalOption implements ReadOption {
    private final long limit;

    LimitOption(long limit) {
      this.limit = limit;
    }

    @Override
    void appendToOptions(Options options) {
      options.limit = limit;
    }
  }

  static class OrderByOption extends InternalOption implements ReadOption {
    private final RpcOrderBy orderBy;

    OrderByOption(RpcOrderBy orderBy) {
      this.orderBy = orderBy;
    }

    @Override
    void appendToOptions(Options options) {
      options.orderBy = orderBy;
    }
  }

  static final class DataBoostQueryOption extends InternalOption implements ReadAndQueryOption {

    private final Boolean dataBoostEnabled;

    DataBoostQueryOption(Boolean dataBoostEnabled) {
      this.dataBoostEnabled = dataBoostEnabled;
    }

    @Override
    void appendToOptions(Options options) {
      options.dataBoostEnabled = dataBoostEnabled;
    }
  }

  static class PageSizeOption extends InternalOption implements ListOption {
    private final int pageSize;

    PageSizeOption(int pageSize) {
      this.pageSize = pageSize;
    }

    @Override
    void appendToOptions(Options options) {
      options.pageSize = pageSize;
    }
  }

  static class PageTokenOption extends InternalOption implements ListOption {
    private final String pageToken;

    PageTokenOption(String pageToken) {
      this.pageToken = pageToken;
    }

    @Override
    void appendToOptions(Options options) {
      options.pageToken = pageToken;
    }
  }

  static class FilterOption extends InternalOption implements ListOption {
    private final String filter;

    FilterOption(String filter) {
      this.filter = filter;
    }

    @Override
    void appendToOptions(Options options) {
      options.filter = filter;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) return true;
      if (!(o instanceof FilterOption)) return false;
      return Objects.equals(filter, ((FilterOption) o).filter);
    }
  }
}
