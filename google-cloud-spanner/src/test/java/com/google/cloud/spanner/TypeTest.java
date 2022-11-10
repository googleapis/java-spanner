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

import static com.google.cloud.spanner.Type.StructField;
import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.google.cloud.spanner.Type.Code;
import com.google.spanner.v1.TypeAnnotationCode;
import com.google.spanner.v1.TypeCode;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link com.google.cloud.spanner.Type}. */
@RunWith(JUnit4.class)
public class TypeTest {

  private abstract static class ScalarTypeTester {
    private final Type.Code expectedCode;
    private final TypeCode expectedTypeCode;
    private final TypeAnnotationCode expectedTypeAnnotationCode;
    private String protoTypeFqn = "";

    ScalarTypeTester(Type.Code expectedCode, TypeCode expectedTypeCode) {
      this(expectedCode, expectedTypeCode, TypeAnnotationCode.TYPE_ANNOTATION_CODE_UNSPECIFIED);
    }

    ScalarTypeTester(
        Type.Code expectedCode,
        TypeCode expectedTypeCode,
        TypeAnnotationCode expectedTypeAnnotationCode) {
      this.expectedCode = expectedCode;
      this.expectedTypeCode = expectedTypeCode;
      this.expectedTypeAnnotationCode = expectedTypeAnnotationCode;
    }

    ScalarTypeTester(Type.Code expectedCode, TypeCode expectedTypeCode, String protoTypeFqn) {
      this(expectedCode, expectedTypeCode);
      this.protoTypeFqn = protoTypeFqn;
    }

    abstract Type newType();

    void test() {
      Type t = newType();
      assertThat(t.getCode()).isEqualTo(expectedCode);
      assertThat(newType()).isEqualTo(t); // Interned.
      // String form is deliberately the same as the corresponding type enum in the public API.
      if (expectedTypeAnnotationCode != TypeAnnotationCode.TYPE_ANNOTATION_CODE_UNSPECIFIED) {
        assertThat(t.toString())
            .isEqualTo(
                expectedTypeCode.toString() + "<" + expectedTypeAnnotationCode.toString() + ">");
      } else {
        assertThat(t.toString()).isEqualTo(expectedTypeCode.toString());
      }

      com.google.spanner.v1.Type proto = t.toProto();
      assertThat(proto.getCode()).isEqualTo(expectedTypeCode);
      assertThat(proto.getTypeAnnotation()).isEqualTo(expectedTypeAnnotationCode);
      assertThat(proto.getProtoTypeFqn()).isEqualTo(protoTypeFqn);
      assertThat(proto.hasArrayElementType()).isFalse();
      assertThat(proto.hasStructType()).isFalse();

      // Round trip.
      Type fromProto = Type.fromProto(proto);
      assertThat(fromProto).isEqualTo(t);

      reserializeAndAssert(t);
    }
  }

  @Test
  public void bool() {
    new ScalarTypeTester(Type.Code.BOOL, TypeCode.BOOL) {
      @Override
      Type newType() {
        return Type.bool();
      }
    }.test();
  }

  @Test
  public void int64() {
    new ScalarTypeTester(Type.Code.INT64, TypeCode.INT64) {
      @Override
      Type newType() {
        return Type.int64();
      }
    }.test();
  }

  @Test
  public void float64() {
    new ScalarTypeTester(Type.Code.FLOAT64, TypeCode.FLOAT64) {
      @Override
      Type newType() {
        return Type.float64();
      }
    }.test();
  }

  @Test
  public void numeric() {
    new ScalarTypeTester(Type.Code.NUMERIC, TypeCode.NUMERIC) {
      @Override
      Type newType() {
        return Type.numeric();
      }
    }.test();
  }

  @Test
  public void pgNumeric() {
    new ScalarTypeTester(Type.Code.PG_NUMERIC, TypeCode.NUMERIC, TypeAnnotationCode.PG_NUMERIC) {
      @Override
      Type newType() {
        return Type.pgNumeric();
      }
    }.test();
  }

  @Test
  public void string() {
    new ScalarTypeTester(Type.Code.STRING, TypeCode.STRING) {
      @Override
      Type newType() {
        return Type.string();
      }
    }.test();
  }

  @Test
  public void json() {
    new ScalarTypeTester(Code.JSON, TypeCode.JSON) {
      @Override
      Type newType() {
        return Type.json();
      }
    }.test();
  }

  @Test
  public void pgJsonb() {
    new ScalarTypeTester(Code.PG_JSONB, TypeCode.JSON, TypeAnnotationCode.PG_JSONB) {
      @Override
      Type newType() {
        return Type.pgJsonb();
      }
    }.test();
  }

  @Test
  public void bytes() {
    new ScalarTypeTester(Type.Code.BYTES, TypeCode.BYTES) {
      @Override
      Type newType() {
        return Type.bytes();
      }
    }.test();
  }

  @Test
  public void proto() {
    new ScalarTypeTester(Type.Code.PROTO, TypeCode.PROTO, "com.google.temp") {
      @Override
      Type newType() {
        return Type.proto("com.google.temp");
      }
    }.test();
  }

  @Test
  public void protoEnum() {
    new ScalarTypeTester(Type.Code.ENUM, TypeCode.ENUM, "com.google.temp.enum") {
      @Override
      Type newType() {
        return Type.protoEnum("com.google.temp.enum");
      }
    }.test();
  }

  @Test
  public void timestamp() {
    new ScalarTypeTester(Type.Code.TIMESTAMP, TypeCode.TIMESTAMP) {
      @Override
      Type newType() {
        return Type.timestamp();
      }
    }.test();
  }

  @Test
  public void date() {
    new ScalarTypeTester(Type.Code.DATE, TypeCode.DATE) {
      @Override
      Type newType() {
        return Type.date();
      }
    }.test();
  }

  abstract static class ArrayTypeTester {
    private final Type.Code expectedElementCode;
    private final TypeCode expectedElementTypeCode;
    private final TypeAnnotationCode expectedTypeAnnotationCode;
    private final boolean expectInterned;
    private String protoTypeFqn = "";

    ArrayTypeTester(
        Type.Code expectedElementCode, TypeCode expectedElementTypeCode, boolean expectInterned) {
      this(
          expectedElementCode,
          expectedElementTypeCode,
          TypeAnnotationCode.TYPE_ANNOTATION_CODE_UNSPECIFIED,
          expectInterned);
    }

    ArrayTypeTester(
        Type.Code expectedElementCode,
        TypeCode expectedElementTypeCode,
        String protoTypeFqn,
        boolean expectInterned) {
      this(
          expectedElementCode,
          expectedElementTypeCode,
          TypeAnnotationCode.TYPE_ANNOTATION_CODE_UNSPECIFIED,
          expectInterned);
      this.protoTypeFqn = protoTypeFqn;
    }

    ArrayTypeTester(
        Type.Code expectedElementCode,
        TypeCode expectedElementTypeCode,
        TypeAnnotationCode expectedTypeAnnotationCode,
        boolean expectInterned) {
      this.expectedElementCode = expectedElementCode;
      this.expectedElementTypeCode = expectedElementTypeCode;
      this.expectedTypeAnnotationCode = expectedTypeAnnotationCode;
      this.expectInterned = expectInterned;
    }

    abstract Type newElementType();

    void test() {
      Type elementType = newElementType();
      Type t = Type.array(elementType);
      assertThat(t.getCode()).isEqualTo(Type.Code.ARRAY);
      assertThat(t.getArrayElementType()).isEqualTo(elementType);
      if (expectInterned) {
        assertThat(Type.array(newElementType())).isSameInstanceAs(t);
      }
      assertThat(t.toString()).isEqualTo("ARRAY<" + elementType.toString() + ">");

      com.google.spanner.v1.Type proto = t.toProto();
      assertThat(proto.getCode()).isEqualTo(TypeCode.ARRAY);
      assertThat(proto.getArrayElementType()).isEqualTo(elementType.toProto());
      assertThat(proto.hasStructType()).isFalse();

      Type fromProto = Type.fromProto(proto);
      assertThat(fromProto).isEqualTo(t);

      if (expectInterned) {
        assertThat(fromProto).isSameInstanceAs(t);
      }
      reserializeAndAssert(t);
    }
  }

  @Test
  public void boolArray() {
    new ArrayTypeTester(Type.Code.BOOL, TypeCode.BOOL, true) {
      @Override
      Type newElementType() {
        return Type.bool();
      }
    }.test();
  }

  @Test
  public void int64Array() {
    new ArrayTypeTester(Type.Code.INT64, TypeCode.INT64, true) {
      @Override
      Type newElementType() {
        return Type.int64();
      }
    }.test();
  }

  @Test
  public void float64Array() {
    new ArrayTypeTester(Type.Code.FLOAT64, TypeCode.FLOAT64, true) {
      @Override
      Type newElementType() {
        return Type.float64();
      }
    }.test();
  }

  @Test
  public void numericArray() {
    new ArrayTypeTester(Type.Code.NUMERIC, TypeCode.NUMERIC, true) {
      @Override
      Type newElementType() {
        return Type.numeric();
      }
    }.test();
  }

  @Test
  public void pgNumericArray() {
    new ArrayTypeTester(
        Type.Code.PG_NUMERIC, TypeCode.NUMERIC, TypeAnnotationCode.PG_NUMERIC, true) {
      @Override
      Type newElementType() {
        return Type.pgNumeric();
      }
    }.test();
  }

  @Test
  public void stringArray() {
    new ArrayTypeTester(Type.Code.STRING, TypeCode.STRING, true) {
      @Override
      Type newElementType() {
        return Type.string();
      }
    }.test();
  }

  @Test
  public void jsonArray() {
    new ArrayTypeTester(Code.JSON, TypeCode.JSON, true) {
      @Override
      Type newElementType() {
        return Type.json();
      }
    }.test();
  }

  @Test
  public void pgJsonbArray() {
    new ArrayTypeTester(Code.PG_JSONB, TypeCode.JSON, TypeAnnotationCode.PG_JSONB, true) {
      @Override
      Type newElementType() {
        return Type.pgJsonb();
      }
    }.test();
  }

  @Test
  public void bytesArray() {
    new ArrayTypeTester(Type.Code.BYTES, TypeCode.BYTES, true) {
      @Override
      Type newElementType() {
        return Type.bytes();
      }
    }.test();
  }

  @Test
  public void timestampArray() {
    new ArrayTypeTester(Type.Code.TIMESTAMP, TypeCode.TIMESTAMP, true) {
      @Override
      Type newElementType() {
        return Type.timestamp();
      }
    }.test();
  }

  @Test
  public void dateArray() {
    new ArrayTypeTester(Type.Code.DATE, TypeCode.DATE, true) {
      @Override
      Type newElementType() {
        return Type.date();
      }
    }.test();
  }

  @Test
  public void protoArray() {
    new ArrayTypeTester(Type.Code.PROTO, TypeCode.PROTO, "com.google.temp", false) {
      @Override
      Type newElementType() {
        return Type.proto("com.google.temp");
      }
    }.test();
  }

  @Test
  public void protoEnumArray() {
    new ArrayTypeTester(Type.Code.ENUM, TypeCode.ENUM, "com.google.temp.enum", false) {
      @Override
      Type newElementType() {
        return Type.protoEnum("com.google.temp.enum");
      }
    }.test();
  }

  @Test
  public void arrayOfArray() {
    new ArrayTypeTester(Type.Code.ARRAY, TypeCode.ARRAY, false /* not interned */) {
      @Override
      Type newElementType() {
        return Type.array(Type.int64());
      }
    }.test();
  }

  @Test
  public void struct() {
    Type t =
        Type.struct(
            StructField.of("f1", Type.int64()),
            StructField.of("f2", Type.string()),
            StructField.of("f3", Type.pgNumeric()));
    assertThat(t.getCode()).isEqualTo(Type.Code.STRUCT);
    // Exercise StructField equality.
    assertThat(t.getStructFields())
        .containsExactly(
            StructField.of("f1", Type.int64()),
            StructField.of("f2", Type.string()),
            StructField.of("f3", Type.pgNumeric()))
        .inOrder();
    // Exercise StructField getters.
    assertThat(t.getStructFields().get(0).getName()).isEqualTo("f1");
    assertThat(t.getStructFields().get(0).getType()).isEqualTo(Type.int64());
    assertThat(t.getStructFields().get(1).getName()).isEqualTo("f2");
    assertThat(t.getStructFields().get(1).getType()).isEqualTo(Type.string());
    assertThat(t.getStructFields().get(2).getName()).isEqualTo("f3");
    assertThat(t.getStructFields().get(2).getType()).isEqualTo(Type.pgNumeric());
    assertThat(t.toString()).isEqualTo("STRUCT<f1 INT64, f2 STRING, f3 NUMERIC<PG_NUMERIC>>");
    assertThat(t.getFieldIndex("f1")).isEqualTo(0);
    assertThat(t.getFieldIndex("f2")).isEqualTo(1);
    assertThat(t.getFieldIndex("f3")).isEqualTo(2);

    assertProtoEquals(
        t.toProto(),
        "code: STRUCT struct_type { fields { name: 'f1' type { code: INT64 } }"
            + " fields { name: 'f2' type { code: STRING } } "
            + " fields { name: 'f3' type { code: NUMERIC, type_annotation: PG_NUMERIC } } }");
  }

  @Test
  public void emptyStruct() {
    Type t = Type.struct();
    assertThat(t.getCode()).isEqualTo(Type.Code.STRUCT);
    assertThat(t.getStructFields()).isEmpty();
    assertThat(t.toString()).isEqualTo("STRUCT<>");
    assertProtoEquals(t.toProto(), "code: STRUCT struct_type {}");
  }

  @Test
  public void structFieldIndexNotFound() {
    Type t = Type.struct(StructField.of("f1", Type.int64()));
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> t.getFieldIndex("f2"));
    assertThat(e.getMessage().contains("Field not found: f2"));
  }

  @Test
  public void structFieldIndexAmbiguous() {
    Type t = Type.struct(StructField.of("f1", Type.int64()), StructField.of("f1", Type.string()));
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> t.getFieldIndex("f1"));
    assertThat(e.getMessage().contains("Ambiguous field name: f1"));
  }

  @Test
  public void parseErrorMissingTypeCode() {
    com.google.spanner.v1.Type proto = com.google.spanner.v1.Type.newBuilder().build();
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> Type.fromProto(proto));
    assertNotNull(e.getMessage());
  }

  @Test
  public void parseErrorMissingArrayElementTypeProto() {
    com.google.spanner.v1.Type proto =
        com.google.spanner.v1.Type.newBuilder().setCode(TypeCode.ARRAY).build();
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> Type.fromProto(proto));
    assertNotNull(e.getMessage());
  }

  private static void assertProtoEquals(com.google.spanner.v1.Type proto, String expected) {
    MatcherAssert.assertThat(
        proto, SpannerMatchers.matchesProto(com.google.spanner.v1.Type.class, expected));
  }
}
