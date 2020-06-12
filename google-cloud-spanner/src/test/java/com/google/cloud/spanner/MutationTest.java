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

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link com.google.cloud.spanner.Mutation}. */
@RunWith(JUnit4.class)
public class MutationTest {

  @Test
  public void insertEmpty() {
    Mutation m = Mutation.newInsertBuilder("T1").build();
    assertThat(m.getTable()).isEqualTo("T1");
    assertThat(m.getOperation()).isEqualTo(Mutation.Op.INSERT);
    assertThat(m.getColumns()).isEmpty();
    assertThat(m.getValues()).isEmpty();
    assertThat(m.toString()).isEqualTo("insert(T1{})");
  }

  @Test
  public void insert() {
    Mutation m = Mutation.newInsertBuilder("T1").set("C1").to(true).set("C2").to(1234).build();
    assertThat(m.getTable()).isEqualTo("T1");
    assertThat(m.getOperation()).isEqualTo(Mutation.Op.INSERT);
    assertThat(m.getColumns()).containsExactly("C1", "C2").inOrder();
    assertThat(m.getValues()).containsExactly(Value.bool(true), Value.int64(1234)).inOrder();
    assertThat(m.toString()).isEqualTo("insert(T1{C1=true,C2=1234})");
  }

  @Test
  public void insertOrUpdateEmpty() {
    Mutation m = Mutation.newInsertOrUpdateBuilder("T2").build();
    assertThat(m.getTable()).isEqualTo("T2");
    assertThat(m.getOperation()).isEqualTo(Mutation.Op.INSERT_OR_UPDATE);
    assertThat(m.getColumns()).isEmpty();
    assertThat(m.getValues()).isEmpty();
    assertThat(m.toString()).isEqualTo("insert_or_update(T2{})");
  }

  @Test
  public void insertOrUpdate() {
    Mutation m = Mutation.newInsertOrUpdateBuilder("T1").set("C1").to(true).build();
    assertThat(m.getTable()).isEqualTo("T1");
    assertThat(m.getOperation()).isEqualTo(Mutation.Op.INSERT_OR_UPDATE);
    assertThat(m.getColumns()).containsExactly("C1");
    assertThat(m.getValues()).containsExactly(Value.bool(true));
    assertThat(m.toString()).isEqualTo("insert_or_update(T1{C1=true})");
  }

  @Test
  public void updateEmpty() {
    Mutation m = Mutation.newUpdateBuilder("T2").build();
    assertThat(m.getTable()).isEqualTo("T2");
    assertThat(m.getOperation()).isEqualTo(Mutation.Op.UPDATE);
    assertThat(m.getColumns()).isEmpty();
    assertThat(m.getValues()).isEmpty();
    assertThat(m.toString()).isEqualTo("update(T2{})");
  }

  @Test
  public void update() {
    Mutation m = Mutation.newUpdateBuilder("T1").set("C1").to(true).set("C2").to(1234).build();
    assertThat(m.getTable()).isEqualTo("T1");
    assertThat(m.getOperation()).isEqualTo(Mutation.Op.UPDATE);
    assertThat(m.getColumns()).containsExactly("C1", "C2").inOrder();
    assertThat(m.getValues()).containsExactly(Value.bool(true), Value.int64(1234)).inOrder();
    assertThat(m.toString()).isEqualTo("update(T1{C1=true,C2=1234})");
  }

  @Test
  public void replaceEmpty() {
    Mutation m = Mutation.newReplaceBuilder("T2").build();
    assertThat(m.getTable()).isEqualTo("T2");
    assertThat(m.getOperation()).isEqualTo(Mutation.Op.REPLACE);
    assertThat(m.getColumns()).isEmpty();
    assertThat(m.getValues()).isEmpty();
    assertThat(m.toString()).isEqualTo("replace(T2{})");
  }

  @Test
  public void replace() {
    Mutation m = Mutation.newReplaceBuilder("T1").set("C1").to(true).set("C2").to(1234).build();
    assertThat(m.getTable()).isEqualTo("T1");
    assertThat(m.getOperation()).isEqualTo(Mutation.Op.REPLACE);
    assertThat(m.getColumns()).containsExactly("C1", "C2").inOrder();
    assertThat(m.getValues()).containsExactly(Value.bool(true), Value.int64(1234)).inOrder();
    assertThat(m.toString()).isEqualTo("replace(T1{C1=true,C2=1234})");
  }

  @Test
  public void duplicateColumn() {
    try {
      Mutation.newInsertBuilder("T1").set("C1").to(true).set("C1").to(false).build();
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertThat(ex.getMessage()).contains("Duplicate column");
    }
  }

  @Test
  public void duplicateColumnCaseInsensitive() {
    try {
      Mutation.newInsertBuilder("T1").set("C1").to(true).set("c1").to(false).build();
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertThat(ex.getMessage()).contains("Duplicate column");
    }
  }

  @Test
  public void asMap() {
    Mutation m = Mutation.newInsertBuilder("T").build();
    assertThat(m.asMap()).isEqualTo(ImmutableMap.of());

    m = Mutation.newInsertBuilder("T").set("C1").to(true).set("C2").to(1234).build();
    assertThat(m.asMap())
        .isEqualTo(ImmutableMap.of("C1", Value.bool(true), "C2", Value.int64(1234)));
  }

  @Test
  public void unfinishedBindingV1() {
    Mutation.WriteBuilder b = Mutation.newInsertBuilder("T1");
    b.set("C1");
    try {
      b.build();
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertThat(ex.getMessage()).contains("Incomplete binding for column C1");
    }
  }

  @Test
  public void unfinishedBindingV2() {
    Mutation.WriteBuilder b = Mutation.newInsertBuilder("T1");
    b.set("C1");
    try {
      b.set("C2");
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertThat(ex.getMessage()).contains("Incomplete binding for column C1");
    }
  }

  @Test
  public void notInBinding() {
    ValueBinder<Mutation.WriteBuilder> binder = Mutation.newInsertBuilder("T1").set("C1");
    binder.to(1234);
    try {
      binder.to(5678);
      fail("Expected exception");
    } catch (IllegalStateException ex) {
      assertThat(ex.getMessage()).contains("No binding currently active");
    }
  }

  @Test
  public void delete() {
    KeySet keySet = KeySet.singleKey(Key.of("k1"));
    Mutation m = Mutation.delete("T1", keySet);
    assertThat(m.getOperation()).isEqualTo(Mutation.Op.DELETE);
    assertThat(m.getKeySet()).isEqualTo(keySet);
    assertThat(m.toString()).isEqualTo("delete(T1{[k1]})");
  }

  @Test
  public void equalsAndHashCode() {
    EqualsTester tester = new EqualsTester();

    // Equality, not identity.
    tester.addEqualityGroup(
        Mutation.newInsertBuilder("T1").build(), Mutation.newInsertBuilder("T1").build());

    // Operation types are distinguished.
    tester.addEqualityGroup(Mutation.newInsertOrUpdateBuilder("T1").build());
    tester.addEqualityGroup(Mutation.newUpdateBuilder("T1").build());
    tester.addEqualityGroup(Mutation.newReplaceBuilder("T1").build());

    // Table is distinguished.
    tester.addEqualityGroup(Mutation.newInsertBuilder("T2").build());

    // Columns/values are distinguished (but by equality, not identity).
    tester.addEqualityGroup(
        Mutation.newInsertBuilder("T1").set("C").to("V").build(),
        Mutation.newInsertBuilder("T1").set("C").to("V").build());

    // Deletes consider the key set.
    tester.addEqualityGroup(Mutation.delete("T1", KeySet.all()));
    tester.addEqualityGroup(
        Mutation.delete("T1", KeySet.singleKey(Key.of("k"))), Mutation.delete("T1", Key.of("k")));

    tester.testEquals();
  }

  @Test
  public void serializationBasic() {
    List<Mutation> mutations =
        Arrays.asList(
            Mutation.newInsertBuilder("T").set("C").to("V").build(),
            Mutation.newUpdateBuilder("T").set("C").to("V").build(),
            Mutation.newInsertOrUpdateBuilder("T").set("C").to("V").build(),
            Mutation.newReplaceBuilder("T").set("C").to("V").build(),
            Mutation.delete("T", KeySet.singleKey(Key.of("k"))));

    List<com.google.spanner.v1.Mutation> proto = new ArrayList<>();

    // Include an existing element so that we know toProto() do not clear the list.
    com.google.spanner.v1.Mutation existingProto =
        com.google.spanner.v1.Mutation.getDefaultInstance();
    proto.add(existingProto);

    Mutation.toProto(mutations, proto);

    assertThat(proto.size()).isAtLeast(1);
    assertThat(proto.get(0)).isSameInstanceAs(existingProto);
    proto.remove(0);

    assertThat(proto.size()).isEqualTo(5);
    MatcherAssert.assertThat(
        proto.get(0),
        matchesProto("insert { table: 'T' columns: 'C' values { values { string_value: 'V' } } }"));
    MatcherAssert.assertThat(
        proto.get(1),
        matchesProto("update { table: 'T' columns: 'C' values { values { string_value: 'V' } } }"));
    MatcherAssert.assertThat(
        proto.get(2),
        matchesProto(
            "insert_or_update { table: 'T' columns: 'C'"
                + " values { values { string_value: 'V' } } }"));
    MatcherAssert.assertThat(
        proto.get(3),
        matchesProto(
            "replace { table: 'T' columns: 'C' values { values { string_value: 'V' } } }"));
    MatcherAssert.assertThat(
        proto.get(4),
        matchesProto("delete { table: 'T' key_set { keys { values { string_value: 'k' } } } }"));
  }

  @Test
  public void toProtoCoalescingChangeOfTable() {
    List<Mutation> mutations =
        Arrays.asList(
            Mutation.newInsertBuilder("T1").set("C").to("V1").build(),
            Mutation.newInsertBuilder("T1").set("C").to("V2").build(),
            Mutation.newInsertBuilder("T1").set("C").to("V3").build(),
            Mutation.newInsertBuilder("T2").set("C").to("V4").build(),
            Mutation.newInsertBuilder("T2").set("C").to("V5").build());

    List<com.google.spanner.v1.Mutation> proto = new ArrayList<>();
    Mutation.toProto(mutations, proto);

    assertThat(proto.size()).isEqualTo(2);
    MatcherAssert.assertThat(
        proto.get(0),
        matchesProto(
            "insert { table: 'T1' columns: 'C' values { values { string_value: 'V1' } }"
                + " values { values { string_value: 'V2' } }"
                + " values { values { string_value: 'V3' } } }"));
    MatcherAssert.assertThat(
        proto.get(1),
        matchesProto(
            "insert { table: 'T2' columns: 'C' values { values { string_value: 'V4' } }"
                + " values { values { string_value: 'V5' } } }"));
  }

  @Test
  public void toProtoCoalescingChangeOfOperation() {
    List<Mutation> mutations =
        Arrays.asList(
            Mutation.newInsertBuilder("T").set("C").to("V1").build(),
            Mutation.newInsertBuilder("T").set("C").to("V2").build(),
            Mutation.newInsertBuilder("T").set("C").to("V3").build(),
            Mutation.newUpdateBuilder("T").set("C").to("V4").build(),
            Mutation.newUpdateBuilder("T").set("C").to("V5").build());

    List<com.google.spanner.v1.Mutation> proto = new ArrayList<>();
    Mutation.toProto(mutations, proto);

    assertThat(proto.size()).isEqualTo(2);
    MatcherAssert.assertThat(
        proto.get(0),
        matchesProto(
            "insert { table: 'T' columns: 'C' values { values { string_value: 'V1' } }"
                + " values { values { string_value: 'V2' } }"
                + " values { values { string_value: 'V3' } } }"));
    MatcherAssert.assertThat(
        proto.get(1),
        matchesProto(
            "update { table: 'T' columns: 'C' values { values { string_value: 'V4' } }"
                + " values { values { string_value: 'V5' } } }"));
  }

  @Test
  public void toProtoCoalescingChangeOfColumn() {
    List<Mutation> mutations =
        Arrays.asList(
            Mutation.newInsertBuilder("T").set("C1").to("V1").build(),
            Mutation.newInsertBuilder("T").set("C1").to("V2").build(),
            Mutation.newInsertBuilder("T").set("C1").to("V3").build(),
            Mutation.newInsertBuilder("T").set("C2").to("V4").build(),
            Mutation.newInsertBuilder("T").set("C2").to("V5").build());

    List<com.google.spanner.v1.Mutation> proto = new ArrayList<>();
    Mutation.toProto(mutations, proto);

    assertThat(proto.size()).isEqualTo(2);
    MatcherAssert.assertThat(
        proto.get(0),
        matchesProto(
            "insert { table: 'T' columns: 'C1' values { values { string_value: 'V1' } }"
                + " values { values { string_value: 'V2' } }"
                + " values { values { string_value: 'V3' } } }"));
    MatcherAssert.assertThat(
        proto.get(1),
        matchesProto(
            "insert { table: 'T' columns: 'C2' values { values { string_value: 'V4' } }"
                + " values { values { string_value: 'V5' } } }"));
  }

  @Test
  public void toProtoCoalescingDelete() {
    List<Mutation> mutations =
        Arrays.asList(
            Mutation.delete("T", Key.of("k1")),
            Mutation.delete("T", Key.of("k2")),
            Mutation.delete("T", KeySet.range(KeyRange.closedOpen(Key.of("ka"), Key.of("kb")))),
            Mutation.delete("T", KeySet.range(KeyRange.closedClosed(Key.of("kc"), Key.of("kd")))));

    List<com.google.spanner.v1.Mutation> proto = new ArrayList<>();
    Mutation.toProto(mutations, proto);

    assertThat(proto.size()).isEqualTo(1);
    MatcherAssert.assertThat(
        proto.get(0),
        matchesProto(
            "delete {"
                + "  table: 'T'"
                + "  key_set {"
                + "    keys { values { string_value: 'k1' } }"
                + "    keys { values { string_value: 'k2' } }"
                + "    ranges { start_closed { values { string_value: 'ka' } } "
                + "             end_open { values { string_value: 'kb' } } }"
                + "    ranges { start_closed { values { string_value: 'kc' } } "
                + "             end_closed { values { string_value: 'kd' } } }"
                + "  }"
                + "} "));
  }

  @Test
  public void toProtoCoalescingDeleteChanges() {
    List<Mutation> mutations =
        Arrays.asList(
            Mutation.newInsertBuilder("T1").set("C").to("V1").build(),
            Mutation.delete("T1", Key.of("k1")),
            Mutation.delete("T1", Key.of("k2")),
            Mutation.delete("T2", Key.of("k3")),
            Mutation.delete("T2", Key.of("k4")),
            Mutation.newInsertBuilder("T2").set("C").to("V1").build());

    List<com.google.spanner.v1.Mutation> proto = new ArrayList<>();
    Mutation.toProto(mutations, proto);

    assertThat(proto.size()).isEqualTo(4);
    MatcherAssert.assertThat(
        proto.get(0),
        matchesProto(
            "insert { table: 'T1' columns: 'C' values { values { string_value: 'V1' } } }"));
    MatcherAssert.assertThat(
        proto.get(1),
        matchesProto(
            "delete { table: 'T1' key_set { keys { values { string_value: 'k1' } } "
                + "keys { values { string_value: 'k2' } } } }"));
    MatcherAssert.assertThat(
        proto.get(2),
        matchesProto(
            "delete { table: 'T2' key_set { keys { values { string_value: 'k3' } } "
                + "keys { values { string_value: 'k4' } } } }"));
    MatcherAssert.assertThat(
        proto.get(3),
        matchesProto(
            "insert { table: 'T2', columns: 'C', values { values { string_value: 'V1' } } }"));
  }

  @Test
  public void javaSerialization() throws Exception {
    reserializeAndAssert(appendAllTypes(Mutation.newInsertBuilder("test")).build());
    reserializeAndAssert(appendAllTypes(Mutation.newUpdateBuilder("test")).build());
    reserializeAndAssert(appendAllTypes(Mutation.newReplaceBuilder("test")).build());
    reserializeAndAssert(appendAllTypes(Mutation.newInsertOrUpdateBuilder("test")).build());

    reserializeAndAssert(
        Mutation.delete(
            "test",
            Key.of(
                "one",
                2,
                null,
                true,
                2.3,
                ByteArray.fromBase64("abcd"),
                Timestamp.ofTimeSecondsAndNanos(1, 2),
                Date.fromYearMonthDay(2017, 04, 17))));
    reserializeAndAssert(Mutation.delete("test", KeySet.all()));
    reserializeAndAssert(
        Mutation.delete(
            "test",
            KeySet.newBuilder()
                .addRange(KeyRange.closedClosed(Key.of("one", 2, null), Key.of("two", 3, null)))
                .build()));
    reserializeAndAssert(
        Mutation.delete(
            "test",
            KeySet.newBuilder()
                .addRange(KeyRange.closedOpen(Key.of("one", 2, null), Key.of("two", 3, null)))
                .build()));
    reserializeAndAssert(
        Mutation.delete(
            "test",
            KeySet.newBuilder()
                .addRange(KeyRange.openClosed(Key.of("one", 2, null), Key.of("two", 3, null)))
                .build()));
    reserializeAndAssert(
        Mutation.delete(
            "test",
            KeySet.newBuilder()
                .addRange(KeyRange.openOpen(Key.of("one", 2, null), Key.of("two", 3, null)))
                .build()));
  }

  private Mutation.WriteBuilder appendAllTypes(Mutation.WriteBuilder builder) {
    return builder
        .set("bool")
        .to(true)
        .set("boolNull")
        .to((Boolean) null)
        .set("int")
        .to(42)
        .set("intNull")
        .to((Long) null)
        .set("float")
        .to(42.1)
        .set("floatNull")
        .to((Double) null)
        .set("string")
        .to("str")
        .set("stringNull")
        .to((String) null)
        .set("boolArr")
        .toBoolArray(new boolean[] {true, false})
        .set("boolArrNull")
        .toBoolArray((boolean[]) null)
        .set("intArr")
        .toInt64Array(new long[] {1, 2, 3})
        .set("intArrNull")
        .toInt64Array((long[]) null)
        .set("floatArr")
        .toFloat64Array(new double[] {1.1, 2.2, 3.3})
        .set("floatArrNull")
        .toFloat64Array((double[]) null)
        .set("nullStr")
        .to((String) null)
        .set("timestamp")
        .to(Timestamp.MAX_VALUE)
        .set("timestampNull")
        .to((Timestamp) null)
        .set("date")
        .to(Date.fromYearMonthDay(2017, 04, 17))
        .set("dateNull")
        .to((Date) null)
        .set("stringArr")
        .toStringArray(ImmutableList.of("one", "two"))
        .set("stringArrNull")
        .toStringArray(null)
        .set("timestampArr")
        .toTimestampArray(ImmutableList.of(Timestamp.MAX_VALUE, Timestamp.MAX_VALUE))
        .set("timestampArrNull")
        .toTimestampArray(null)
        .set("dateArr")
        .toDateArray(
            ImmutableList.of(
                Date.fromYearMonthDay(2017, 04, 17), Date.fromYearMonthDay(2017, 04, 18)))
        .set("dateArrNull")
        .toDateArray(null);
  }

  static Matcher<com.google.spanner.v1.Mutation> matchesProto(String expected) {
    return SpannerMatchers.matchesProto(com.google.spanner.v1.Mutation.class, expected);
  }
}
