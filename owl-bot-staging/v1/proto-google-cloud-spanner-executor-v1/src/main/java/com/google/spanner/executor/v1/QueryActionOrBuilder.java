// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/executor/v1/cloud_executor.proto

package com.google.spanner.executor.v1;

public interface QueryActionOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.spanner.executor.v1.QueryAction)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * The SQL string.
   * </pre>
   *
   * <code>string sql = 1;</code>
   * @return The sql.
   */
  java.lang.String getSql();
  /**
   * <pre>
   * The SQL string.
   * </pre>
   *
   * <code>string sql = 1;</code>
   * @return The bytes for sql.
   */
  com.google.protobuf.ByteString
      getSqlBytes();

  /**
   * <pre>
   * Parameters for the SQL string.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.QueryAction.Parameter params = 2;</code>
   */
  java.util.List<com.google.spanner.executor.v1.QueryAction.Parameter> 
      getParamsList();
  /**
   * <pre>
   * Parameters for the SQL string.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.QueryAction.Parameter params = 2;</code>
   */
  com.google.spanner.executor.v1.QueryAction.Parameter getParams(int index);
  /**
   * <pre>
   * Parameters for the SQL string.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.QueryAction.Parameter params = 2;</code>
   */
  int getParamsCount();
  /**
   * <pre>
   * Parameters for the SQL string.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.QueryAction.Parameter params = 2;</code>
   */
  java.util.List<? extends com.google.spanner.executor.v1.QueryAction.ParameterOrBuilder> 
      getParamsOrBuilderList();
  /**
   * <pre>
   * Parameters for the SQL string.
   * </pre>
   *
   * <code>repeated .google.spanner.executor.v1.QueryAction.Parameter params = 2;</code>
   */
  com.google.spanner.executor.v1.QueryAction.ParameterOrBuilder getParamsOrBuilder(
      int index);
}
