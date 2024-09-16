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

// Protobuf Java Version: 3.25.4
package com.google.spanner.admin.database.v1;

public interface ListDatabasesResponseOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.admin.database.v1.ListDatabasesResponse)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Databases that matched the request.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Database databases = 1;</code>
   */
  java.util.List<com.google.spanner.admin.database.v1.Database> getDatabasesList();
  /**
   *
   *
   * <pre>
   * Databases that matched the request.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Database databases = 1;</code>
   */
  com.google.spanner.admin.database.v1.Database getDatabases(int index);
  /**
   *
   *
   * <pre>
   * Databases that matched the request.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Database databases = 1;</code>
   */
  int getDatabasesCount();
  /**
   *
   *
   * <pre>
   * Databases that matched the request.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Database databases = 1;</code>
   */
  java.util.List<? extends com.google.spanner.admin.database.v1.DatabaseOrBuilder>
      getDatabasesOrBuilderList();
  /**
   *
   *
   * <pre>
   * Databases that matched the request.
   * </pre>
   *
   * <code>repeated .google.spanner.admin.database.v1.Database databases = 1;</code>
   */
  com.google.spanner.admin.database.v1.DatabaseOrBuilder getDatabasesOrBuilder(int index);

  /**
   *
   *
   * <pre>
   * `next_page_token` can be sent in a subsequent
   * [ListDatabases][google.spanner.admin.database.v1.DatabaseAdmin.ListDatabases]
   * call to fetch more of the matching databases.
   * </pre>
   *
   * <code>string next_page_token = 2;</code>
   *
   * @return The nextPageToken.
   */
  java.lang.String getNextPageToken();
  /**
   *
   *
   * <pre>
   * `next_page_token` can be sent in a subsequent
   * [ListDatabases][google.spanner.admin.database.v1.DatabaseAdmin.ListDatabases]
   * call to fetch more of the matching databases.
   * </pre>
   *
   * <code>string next_page_token = 2;</code>
   *
   * @return The bytes for nextPageToken.
   */
  com.google.protobuf.ByteString getNextPageTokenBytes();
}
