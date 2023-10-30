/*
 * Copyright 2023 Google LLC
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
// source: google/spanner/v1/type.proto

package com.google.spanner.v1;

/**
 *
 *
 * <pre>
 * `TypeAnnotationCode` is used as a part of [Type][google.spanner.v1.Type] to
 * disambiguate SQL types that should be used for a given Cloud Spanner value.
 * Disambiguation is needed because the same Cloud Spanner type can be mapped to
 * different SQL types depending on SQL dialect. TypeAnnotationCode doesn't
 * affect the way value is serialized.
 * </pre>
 *
 * Protobuf enum {@code google.spanner.v1.TypeAnnotationCode}
 */
public enum TypeAnnotationCode implements com.google.protobuf.ProtocolMessageEnum {
  /**
   *
   *
   * <pre>
   * Not specified.
   * </pre>
   *
   * <code>TYPE_ANNOTATION_CODE_UNSPECIFIED = 0;</code>
   */
  TYPE_ANNOTATION_CODE_UNSPECIFIED(0),
  /**
   *
   *
   * <pre>
   * PostgreSQL compatible NUMERIC type. This annotation needs to be applied to
   * [Type][google.spanner.v1.Type] instances having [NUMERIC][google.spanner.v1.TypeCode.NUMERIC]
   * type code to specify that values of this type should be treated as
   * PostgreSQL NUMERIC values. Currently this annotation is always needed for
   * [NUMERIC][google.spanner.v1.TypeCode.NUMERIC] when a client interacts with PostgreSQL-enabled
   * Spanner databases.
   * </pre>
   *
   * <code>PG_NUMERIC = 2;</code>
   */
  PG_NUMERIC(2),
  /**
   *
   *
   * <pre>
   * PostgreSQL compatible JSONB type. This annotation needs to be applied to
   * [Type][google.spanner.v1.Type] instances having [JSON][google.spanner.v1.TypeCode.JSON]
   * type code to specify that values of this type should be treated as
   * PostgreSQL JSONB values. Currently this annotation is always needed for
   * [JSON][google.spanner.v1.TypeCode.JSON] when a client interacts with PostgreSQL-enabled
   * Spanner databases.
   * </pre>
   *
   * <code>PG_JSONB = 3;</code>
   */
  PG_JSONB(3),
  UNRECOGNIZED(-1),
  ;

  /**
   *
   *
   * <pre>
   * Not specified.
   * </pre>
   *
   * <code>TYPE_ANNOTATION_CODE_UNSPECIFIED = 0;</code>
   */
  public static final int TYPE_ANNOTATION_CODE_UNSPECIFIED_VALUE = 0;
  /**
   *
   *
   * <pre>
   * PostgreSQL compatible NUMERIC type. This annotation needs to be applied to
   * [Type][google.spanner.v1.Type] instances having [NUMERIC][google.spanner.v1.TypeCode.NUMERIC]
   * type code to specify that values of this type should be treated as
   * PostgreSQL NUMERIC values. Currently this annotation is always needed for
   * [NUMERIC][google.spanner.v1.TypeCode.NUMERIC] when a client interacts with PostgreSQL-enabled
   * Spanner databases.
   * </pre>
   *
   * <code>PG_NUMERIC = 2;</code>
   */
  public static final int PG_NUMERIC_VALUE = 2;
  /**
   *
   *
   * <pre>
   * PostgreSQL compatible JSONB type. This annotation needs to be applied to
   * [Type][google.spanner.v1.Type] instances having [JSON][google.spanner.v1.TypeCode.JSON]
   * type code to specify that values of this type should be treated as
   * PostgreSQL JSONB values. Currently this annotation is always needed for
   * [JSON][google.spanner.v1.TypeCode.JSON] when a client interacts with PostgreSQL-enabled
   * Spanner databases.
   * </pre>
   *
   * <code>PG_JSONB = 3;</code>
   */
  public static final int PG_JSONB_VALUE = 3;

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
  public static TypeAnnotationCode valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static TypeAnnotationCode forNumber(int value) {
    switch (value) {
      case 0:
        return TYPE_ANNOTATION_CODE_UNSPECIFIED;
      case 2:
        return PG_NUMERIC;
      case 3:
        return PG_JSONB;
      default:
        return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<TypeAnnotationCode> internalGetValueMap() {
    return internalValueMap;
  }

  private static final com.google.protobuf.Internal.EnumLiteMap<TypeAnnotationCode>
      internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<TypeAnnotationCode>() {
            public TypeAnnotationCode findValueByNumber(int number) {
              return TypeAnnotationCode.forNumber(number);
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
    return com.google.spanner.v1.TypeProto.getDescriptor().getEnumTypes().get(1);
  }

  private static final TypeAnnotationCode[] VALUES = values();

  public static TypeAnnotationCode valueOf(
      com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException("EnumValueDescriptor is not for this type.");
    }
    if (desc.getIndex() == -1) {
      return UNRECOGNIZED;
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private TypeAnnotationCode(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:google.spanner.v1.TypeAnnotationCode)
}
