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

// Protobuf Java Version: 3.25.2
package com.google.spanner.admin.database.v1;

public interface CreateDatabaseRequestOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.database.v1.CreateDatabaseRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Required. The name of the instance that will serve the new database.
   * Values are of the form `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * </pre>
   *
   * <code>
   * string parent = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The parent.
   */
  java.lang.String getParent();
  /**
   *
   *
   * <pre>
   * Required. The name of the instance that will serve the new database.
   * Values are of the form `projects/&lt;project&gt;/instances/&lt;instance&gt;`.
   * </pre>
   *
   * <code>
   * string parent = 1 [(.google.api.field_behavior) = REQUIRED, (.google.api.resource_reference) = { ... }
   * </code>
   *
   * @return The bytes for parent.
   */
  com.google.protobuf.ByteString getParentBytes();

  /**
   *
   *
   * <pre>
   * Required. A `CREATE DATABASE` statement, which specifies the ID of the
   * new database.  The database ID must conform to the regular expression
   * `[a-z][a-z0-9_&#92;-]*[a-z0-9]` and be between 2 and 30 characters in length.
   * If the database ID is a reserved word or if it contains a hyphen, the
   * database ID must be enclosed in backticks (`` ` ``).
   * </pre>
   *
   * <code>string create_statement = 2 [(.google.api.field_behavior) = REQUIRED];</code>
   *
   * @return The createStatement.
   */
  java.lang.String getCreateStatement();
  /**
   *
   *
   * <pre>
   * Required. A `CREATE DATABASE` statement, which specifies the ID of the
   * new database.  The database ID must conform to the regular expression
   * `[a-z][a-z0-9_&#92;-]*[a-z0-9]` and be between 2 and 30 characters in length.
   * If the database ID is a reserved word or if it contains a hyphen, the
   * database ID must be enclosed in backticks (`` ` ``).
   * </pre>
   *
   * <code>string create_statement = 2 [(.google.api.field_behavior) = REQUIRED];</code>
   *
   * @return The bytes for createStatement.
   */
  com.google.protobuf.ByteString getCreateStatementBytes();

  /**
   *
   *
   * <pre>
   * Optional. A list of DDL statements to run inside the newly created
   * database. Statements can create tables, indexes, etc. These
   * statements execute atomically with the creation of the database:
   * if there is an error in any statement, the database is not created.
   * </pre>
   *
   * <code>repeated string extra_statements = 3 [(.google.api.field_behavior) = OPTIONAL];</code>
   *
   * @return A list containing the extraStatements.
   */
  java.util.List<java.lang.String> getExtraStatementsList();
  /**
   *
   *
   * <pre>
   * Optional. A list of DDL statements to run inside the newly created
   * database. Statements can create tables, indexes, etc. These
   * statements execute atomically with the creation of the database:
   * if there is an error in any statement, the database is not created.
   * </pre>
   *
   * <code>repeated string extra_statements = 3 [(.google.api.field_behavior) = OPTIONAL];</code>
   *
   * @return The count of extraStatements.
   */
  int getExtraStatementsCount();
  /**
   *
   *
   * <pre>
   * Optional. A list of DDL statements to run inside the newly created
   * database. Statements can create tables, indexes, etc. These
   * statements execute atomically with the creation of the database:
   * if there is an error in any statement, the database is not created.
   * </pre>
   *
   * <code>repeated string extra_statements = 3 [(.google.api.field_behavior) = OPTIONAL];</code>
   *
   * @param index The index of the element to return.
   * @return The extraStatements at the given index.
   */
  java.lang.String getExtraStatements(int index);
  /**
   *
   *
   * <pre>
   * Optional. A list of DDL statements to run inside the newly created
   * database. Statements can create tables, indexes, etc. These
   * statements execute atomically with the creation of the database:
   * if there is an error in any statement, the database is not created.
   * </pre>
   *
   * <code>repeated string extra_statements = 3 [(.google.api.field_behavior) = OPTIONAL];</code>
   *
   * @param index The index of the value to return.
   * @return The bytes of the extraStatements at the given index.
   */
  com.google.protobuf.ByteString getExtraStatementsBytes(int index);

  /**
   *
   *
   * <pre>
   * Optional. The encryption configuration for the database. If this field is not
   * specified, Cloud Spanner will encrypt/decrypt all data at rest using
   * Google default encryption.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.database.v1.EncryptionConfig encryption_config = 4 [(.google.api.field_behavior) = OPTIONAL];
   * </code>
   *
   * @return Whether the encryptionConfig field is set.
   */
  boolean hasEncryptionConfig();
  /**
   *
   *
   * <pre>
   * Optional. The encryption configuration for the database. If this field is not
   * specified, Cloud Spanner will encrypt/decrypt all data at rest using
   * Google default encryption.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.database.v1.EncryptionConfig encryption_config = 4 [(.google.api.field_behavior) = OPTIONAL];
   * </code>
   *
   * @return The encryptionConfig.
   */
  com.google.spanner.admin.database.v1.EncryptionConfig getEncryptionConfig();
  /**
   *
   *
   * <pre>
   * Optional. The encryption configuration for the database. If this field is not
   * specified, Cloud Spanner will encrypt/decrypt all data at rest using
   * Google default encryption.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.database.v1.EncryptionConfig encryption_config = 4 [(.google.api.field_behavior) = OPTIONAL];
   * </code>
   */
  com.google.spanner.admin.database.v1.EncryptionConfigOrBuilder getEncryptionConfigOrBuilder();

  /**
   *
   *
   * <pre>
   * Optional. The dialect of the Cloud Spanner Database.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.database.v1.DatabaseDialect database_dialect = 5 [(.google.api.field_behavior) = OPTIONAL];
   * </code>
   *
   * @return The enum numeric value on the wire for databaseDialect.
   */
  int getDatabaseDialectValue();
  /**
   *
   *
   * <pre>
   * Optional. The dialect of the Cloud Spanner Database.
   * </pre>
   *
   * <code>
   * .google.spanner.admin.database.v1.DatabaseDialect database_dialect = 5 [(.google.api.field_behavior) = OPTIONAL];
   * </code>
   *
   * @return The databaseDialect.
   */
  com.google.spanner.admin.database.v1.DatabaseDialect getDatabaseDialect();

  /**
   *
   *
   * <pre>
   * Optional. Proto descriptors used by CREATE/ALTER PROTO BUNDLE statements in
   * 'extra_statements' above.
   * Contains a protobuf-serialized
   * [google.protobuf.FileDescriptorSet](https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/descriptor.proto).
   * To generate it, [install](https://grpc.io/docs/protoc-installation/) and
   * run `protoc` with --include_imports and --descriptor_set_out. For example,
   * to generate for moon/shot/app.proto, run
   * ```
   * $protoc  --proto_path=/app_path --proto_path=/lib_path &#92;
   *          --include_imports &#92;
   *          --descriptor_set_out=descriptors.data &#92;
   *          moon/shot/app.proto
   * ```
   * For more details, see protobuffer [self
   * description](https://developers.google.com/protocol-buffers/docs/techniques#self-description).
   * </pre>
   *
   * <code>bytes proto_descriptors = 6 [(.google.api.field_behavior) = OPTIONAL];</code>
   *
   * @return The protoDescriptors.
   */
  com.google.protobuf.ByteString getProtoDescriptors();
}
