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
// source: google/spanner/v1/type.proto

// Protobuf Java Version: 3.25.5
package com.google.spanner.v1;

public interface TypeOrBuilder
    extends
    // @@protoc_insertion_point(interface_extends:google.spanner.v1.Type)
    com.google.protobuf.MessageOrBuilder {

  /**
   *
   *
   * <pre>
   * Required. The [TypeCode][google.spanner.v1.TypeCode] for this type.
   * </pre>
   *
   * <code>.google.spanner.v1.TypeCode code = 1 [(.google.api.field_behavior) = REQUIRED];</code>
   *
   * @return The enum numeric value on the wire for code.
   */
  int getCodeValue();
  /**
   *
   *
   * <pre>
   * Required. The [TypeCode][google.spanner.v1.TypeCode] for this type.
   * </pre>
   *
   * <code>.google.spanner.v1.TypeCode code = 1 [(.google.api.field_behavior) = REQUIRED];</code>
   *
   * @return The code.
   */
  com.google.spanner.v1.TypeCode getCode();

  /**
   *
   *
   * <pre>
   * If [code][google.spanner.v1.Type.code] == [ARRAY][google.spanner.v1.TypeCode.ARRAY], then `array_element_type`
   * is the type of the array elements.
   * </pre>
   *
   * <code>.google.spanner.v1.Type array_element_type = 2;</code>
   *
   * @return Whether the arrayElementType field is set.
   */
  boolean hasArrayElementType();
  /**
   *
   *
   * <pre>
   * If [code][google.spanner.v1.Type.code] == [ARRAY][google.spanner.v1.TypeCode.ARRAY], then `array_element_type`
   * is the type of the array elements.
   * </pre>
   *
   * <code>.google.spanner.v1.Type array_element_type = 2;</code>
   *
   * @return The arrayElementType.
   */
  com.google.spanner.v1.Type getArrayElementType();
  /**
   *
   *
   * <pre>
   * If [code][google.spanner.v1.Type.code] == [ARRAY][google.spanner.v1.TypeCode.ARRAY], then `array_element_type`
   * is the type of the array elements.
   * </pre>
   *
   * <code>.google.spanner.v1.Type array_element_type = 2;</code>
   */
  com.google.spanner.v1.TypeOrBuilder getArrayElementTypeOrBuilder();

  /**
   *
   *
   * <pre>
   * If [code][google.spanner.v1.Type.code] == [STRUCT][google.spanner.v1.TypeCode.STRUCT], then `struct_type`
   * provides type information for the struct's fields.
   * </pre>
   *
   * <code>.google.spanner.v1.StructType struct_type = 3;</code>
   *
   * @return Whether the structType field is set.
   */
  boolean hasStructType();
  /**
   *
   *
   * <pre>
   * If [code][google.spanner.v1.Type.code] == [STRUCT][google.spanner.v1.TypeCode.STRUCT], then `struct_type`
   * provides type information for the struct's fields.
   * </pre>
   *
   * <code>.google.spanner.v1.StructType struct_type = 3;</code>
   *
   * @return The structType.
   */
  com.google.spanner.v1.StructType getStructType();
  /**
   *
   *
   * <pre>
   * If [code][google.spanner.v1.Type.code] == [STRUCT][google.spanner.v1.TypeCode.STRUCT], then `struct_type`
   * provides type information for the struct's fields.
   * </pre>
   *
   * <code>.google.spanner.v1.StructType struct_type = 3;</code>
   */
  com.google.spanner.v1.StructTypeOrBuilder getStructTypeOrBuilder();

  /**
   *
   *
   * <pre>
   * The [TypeAnnotationCode][google.spanner.v1.TypeAnnotationCode] that disambiguates SQL type that Spanner will
   * use to represent values of this type during query processing. This is
   * necessary for some type codes because a single [TypeCode][google.spanner.v1.TypeCode] can be mapped
   * to different SQL types depending on the SQL dialect. [type_annotation][google.spanner.v1.Type.type_annotation]
   * typically is not needed to process the content of a value (it doesn't
   * affect serialization) and clients can ignore it on the read path.
   * </pre>
   *
   * <code>.google.spanner.v1.TypeAnnotationCode type_annotation = 4;</code>
   *
   * @return The enum numeric value on the wire for typeAnnotation.
   */
  int getTypeAnnotationValue();
  /**
   *
   *
   * <pre>
   * The [TypeAnnotationCode][google.spanner.v1.TypeAnnotationCode] that disambiguates SQL type that Spanner will
   * use to represent values of this type during query processing. This is
   * necessary for some type codes because a single [TypeCode][google.spanner.v1.TypeCode] can be mapped
   * to different SQL types depending on the SQL dialect. [type_annotation][google.spanner.v1.Type.type_annotation]
   * typically is not needed to process the content of a value (it doesn't
   * affect serialization) and clients can ignore it on the read path.
   * </pre>
   *
   * <code>.google.spanner.v1.TypeAnnotationCode type_annotation = 4;</code>
   *
   * @return The typeAnnotation.
   */
  com.google.spanner.v1.TypeAnnotationCode getTypeAnnotation();

  /**
   *
   *
   * <pre>
   * If [code][google.spanner.v1.Type.code] ==
   * [PROTO][google.spanner.v1.TypeCode.PROTO] or
   * [code][google.spanner.v1.Type.code] ==
   * [ENUM][google.spanner.v1.TypeCode.ENUM], then `proto_type_fqn` is the fully
   * qualified name of the proto type representing the proto/enum definition.
   * </pre>
   *
   * <code>string proto_type_fqn = 5;</code>
   *
   * @return The protoTypeFqn.
   */
  java.lang.String getProtoTypeFqn();
  /**
   *
   *
   * <pre>
   * If [code][google.spanner.v1.Type.code] ==
   * [PROTO][google.spanner.v1.TypeCode.PROTO] or
   * [code][google.spanner.v1.Type.code] ==
   * [ENUM][google.spanner.v1.TypeCode.ENUM], then `proto_type_fqn` is the fully
   * qualified name of the proto type representing the proto/enum definition.
   * </pre>
   *
   * <code>string proto_type_fqn = 5;</code>
   *
   * @return The bytes for protoTypeFqn.
   */
  com.google.protobuf.ByteString getProtoTypeFqnBytes();
}
