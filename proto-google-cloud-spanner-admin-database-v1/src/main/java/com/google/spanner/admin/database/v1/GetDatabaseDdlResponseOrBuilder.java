/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/spanner/admin/database/v1/spanner_database_admin.proto

package com.google.spanner.admin.database.v1;

public interface GetDatabaseDdlResponseOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.database.v1.GetDatabaseDdlResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * A list of formatted DDL statements defining the schema of the database
   * specified in the request.
   * </pre>
   *
   * <code>repeated string statements = 1;</code>
   *
   * @return A list containing the statements.
   */
  java.util.List<java.lang.String> getStatementsList();
  /**
   *
   *
   * <pre>
   * A list of formatted DDL statements defining the schema of the database
   * specified in the request.
   * </pre>
   *
   * <code>repeated string statements = 1;</code>
   *
   * @return The count of statements.
   */
  int getStatementsCount();
  /**
   *
   *
   * <pre>
   * A list of formatted DDL statements defining the schema of the database
   * specified in the request.
   * </pre>
   *
   * <code>repeated string statements = 1;</code>
   *
   * @param index The index of the element to return.
   * @return The statements at the given index.
   */
  java.lang.String getStatements(int index);
  /**
   *
   *
   * <pre>
   * A list of formatted DDL statements defining the schema of the database
   * specified in the request.
   * </pre>
   *
   * <code>repeated string statements = 1;</code>
   *
   * @param index The index of the value to return.
   * @return The bytes of the statements at the given index.
   */
  com.google.protobuf.ByteString getStatementsBytes(int index);

  /**
   *
   *
   * <pre>
   * Proto descriptors stored in the database.
   * Contains a protobuf-serialized
   * [google.protobuf.FileDescriptorSet](https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/descriptor.proto).
   * For more details, see protobuffer [self
   * description](https://developers.google.com/protocol-buffers/docs/techniques#self-description).
   * </pre>
   *
   * <code>bytes proto_descriptors = 2;</code>
   *
   * @return The protoDescriptors.
   */
  com.google.protobuf.ByteString getProtoDescriptors();
}
