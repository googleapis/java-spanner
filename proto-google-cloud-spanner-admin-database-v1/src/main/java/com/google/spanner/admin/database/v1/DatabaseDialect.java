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
// source: google/spanner/admin/database/v1/common.proto

// Protobuf Java Version: 3.25.3
package com.google.spanner.admin.database.v1;

/**
 *
 *
 * <pre>
 * Indicates the dialect type of a database.
 * </pre>
 *
 * Protobuf enum {@code google.spanner.admin.database.v1.DatabaseDialect}
 */
public enum DatabaseDialect implements com.google.protobuf.ProtocolMessageEnum {
  /**
   *
   *
   * <pre>
   * Default value. This value will create a database with the
   * GOOGLE_STANDARD_SQL dialect.
   * </pre>
   *
   * <code>DATABASE_DIALECT_UNSPECIFIED = 0;</code>
   */
  DATABASE_DIALECT_UNSPECIFIED(0),
  /**
   *
   *
   * <pre>
   * Google standard SQL.
   * </pre>
   *
   * <code>GOOGLE_STANDARD_SQL = 1;</code>
   */
  GOOGLE_STANDARD_SQL(1),
  /**
   *
   *
   * <pre>
   * PostgreSQL supported SQL.
   * </pre>
   *
   * <code>POSTGRESQL = 2;</code>
   */
  POSTGRESQL(2),
  UNRECOGNIZED(-1),
  ;

  /**
   *
   *
   * <pre>
   * Default value. This value will create a database with the
   * GOOGLE_STANDARD_SQL dialect.
   * </pre>
   *
   * <code>DATABASE_DIALECT_UNSPECIFIED = 0;</code>
   */
  public static final int DATABASE_DIALECT_UNSPECIFIED_VALUE = 0;
  /**
   *
   *
   * <pre>
   * Google standard SQL.
   * </pre>
   *
   * <code>GOOGLE_STANDARD_SQL = 1;</code>
   */
  public static final int GOOGLE_STANDARD_SQL_VALUE = 1;
  /**
   *
   *
   * <pre>
   * PostgreSQL supported SQL.
   * </pre>
   *
   * <code>POSTGRESQL = 2;</code>
   */
  public static final int POSTGRESQL_VALUE = 2;

  public final int getNumber() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalArgumentException(
          "Can't get the number of an unknown enum value.");
    }
    return value;
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @java.lang.Deprecated
  public static DatabaseDialect valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static DatabaseDialect forNumber(int value) {
    switch (value) {
      case 0:
        return DATABASE_DIALECT_UNSPECIFIED;
      case 1:
        return GOOGLE_STANDARD_SQL;
      case 2:
        return POSTGRESQL;
      default:
        return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<DatabaseDialect> internalGetValueMap() {
    return internalValueMap;
  }

  private static final com.google.protobuf.Internal.EnumLiteMap<DatabaseDialect> internalValueMap =
      new com.google.protobuf.Internal.EnumLiteMap<DatabaseDialect>() {
        public DatabaseDialect findValueByNumber(int number) {
          return DatabaseDialect.forNumber(number);
        }
      };

  public final com.google.protobuf.Descriptors.EnumValueDescriptor getValueDescriptor() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalStateException(
          "Can't get the descriptor of an unrecognized enum value.");
    }
    return getDescriptor().getValues().get(ordinal());
  }

  public final com.google.protobuf.Descriptors.EnumDescriptor getDescriptorForType() {
    return getDescriptor();
  }

  public static final com.google.protobuf.Descriptors.EnumDescriptor getDescriptor() {
    return com.google.spanner.admin.database.v1.CommonProto.getDescriptor().getEnumTypes().get(0);
  }

  private static final DatabaseDialect[] VALUES = values();

  public static DatabaseDialect valueOf(com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException("EnumValueDescriptor is not for this type.");
    }
    if (desc.getIndex() == -1) {
      return UNRECOGNIZED;
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private DatabaseDialect(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:google.spanner.admin.database.v1.DatabaseDialect)
}
