/*
 * Copyright 2024 Google LLC
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

import static org.junit.Assert.*;

import java.math.BigInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Interval} */
@RunWith(JUnit4.class)
public class IntervalTest {

  @Test
  public void testOfMonths() {
    Interval interval = Interval.ofMonths(10);
    assertEquals(10, interval.getMonths());
    assertEquals(0, interval.getDays());
    assertEquals(0, interval.getMicroseconds());
    assertEquals(0, interval.getNanoFractions());
  }

  @Test
  public void testOfDays() {
    Interval interval = Interval.ofDays(10);
    assertEquals(0, interval.getMonths());
    assertEquals(10, interval.getDays());
    assertEquals(0, interval.getMicroseconds());
    assertEquals(0, interval.getNanoFractions());
  }

  @Test
  public void testOfSeconds() {
    Interval interval = Interval.ofSeconds(10);
    assertEquals(0, interval.getMonths());
    assertEquals(0, interval.getDays());
    assertEquals(10 * Interval.MICROS_PER_SECOND, interval.getMicroseconds());
    assertEquals(0, interval.getNanoFractions());
  }

  @Test
  public void testOfMilliseconds() {
    Interval interval = Interval.ofMilliseconds(10);
    assertEquals(0, interval.getMonths());
    assertEquals(0, interval.getDays());
    assertEquals(10 * Interval.MICROS_PER_MILLI, interval.getMicroseconds());
    assertEquals(0, interval.getNanoFractions());
  }

  @Test
  public void testOfMicroseconds() {
    Interval interval = Interval.ofMicroseconds(10);
    assertEquals(0, interval.getMonths());
    assertEquals(0, interval.getDays());
    assertEquals(10, interval.getMicroseconds());
    assertEquals(0, interval.getNanoFractions());
  }

  @Test
  public void testOfNanoseconds() {
    Interval interval = Interval.ofNanoseconds(BigInteger.valueOf(10));
    assertEquals(0, interval.getMonths());
    assertEquals(0, interval.getDays());
    assertEquals(0, interval.getMicroseconds());
    assertEquals(10, interval.getNanoFractions());
  }

  @Test
  public void testFromMonthsDaysMicroseconds() {
    Interval interval = Interval.fromMonthsDaysMicros(10, 20, 30);
    assertEquals(10, interval.getMonths());
    assertEquals(20, interval.getDays());
    assertEquals(30, interval.getMicroseconds());
    assertEquals(0, interval.getNanoFractions());
  }

  @Test
  public void testFromMonthsDaysNanoseconds() {
    Interval interval = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(1030));
    assertEquals(10, interval.getMonths());
    assertEquals(20, interval.getDays());
    assertEquals(1, interval.getMicroseconds());
    assertEquals(30, interval.getNanoFractions());

    Interval interval2 = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(-1030));
    assertEquals(10, interval2.getMonths());
    assertEquals(20, interval2.getDays());
    assertEquals(-2, interval2.getMicroseconds());
    assertEquals(970, interval2.getNanoFractions());
  }

  @Test
  public void testParseFromString() {
    TestCase[] testCases =
        new TestCase[] {
          new TestCase("P1Y2M3DT12H12M6.789000123S", 14, 3, 43926789000L, (short) 123),
          new TestCase("P1Y2M3DT13H-48M6S", 14, 3, 43926000000L, (short) 0),
          new TestCase("P1Y2M3D", 14, 3, 0, (short) 0),
          new TestCase("P1Y2M", 14, 0, 0, (short) 0),
          new TestCase("P1Y", 12, 0, 0, (short) 0),
          new TestCase("P2M", 2, 0, 0, (short) 0),
          new TestCase("P3D", 0, 3, 0, (short) 0),
          new TestCase("PT4H25M6.7890001S", 0, 0, 15906789000L, (short) 100),
          new TestCase("PT4H25M6S", 0, 0, 15906000000L, (short) 0),
          new TestCase("PT4H30S", 0, 0, 14430000000L, (short) 0),
          new TestCase("PT4H1M", 0, 0, 14460000000L, (short) 0),
          new TestCase("PT5M", 0, 0, 300000000L, (short) 0),
          new TestCase("PT6.789S", 0, 0, 6789000L, (short) 0),
          new TestCase("PT0.123S", 0, 0, 123000L, (short) 0),
          new TestCase("PT.000000123S", 0, 0, 0L, (short) 123),
          new TestCase("P0Y", 0, 0, 0, (short) 0),
          new TestCase("P-1Y-2M-3DT-12H-12M-6.789000123S", -14, -3, -43926789001L, (short) 877),
          new TestCase("P1Y-2M3DT13H-51M6.789S", 10, 3, 43746789000L, (short) 0),
          new TestCase("P-1Y2M-3DT-13H49M-6.789S", -10, -3, -43866789000L, (short) 0),
          new TestCase("P1Y2M3DT-4H25M-6.7890001S", 14, 3, -12906789001L, (short) 900),
        };

    for (TestCase testCase : testCases) {
      Interval interval = Interval.parseFromString(testCase.intervalString);
      assertEquals(testCase.months, interval.getMonths());
      assertEquals(testCase.days, interval.getDays());
      assertEquals(testCase.microseconds, interval.getMicroseconds());
      assertEquals(testCase.nanoFractions, interval.getNanoFractions());
    }
  }

  @Test
  public void testParseFromString_InvalidString() {
    String[] invalidStrings =
        new String[] {
          "invalid",
          "P",
          "PT",
          "P1YM",
          "P1Y2M3D4H5M6S", // Missing T
          "P1Y2M3DT4H5M6.S", // Missing decimal value
          "P1Y2M3DT4H5M6.789SS", // Extra S
          "P1Y2M3DT4H5M6.", // Missing value after decimal point
          "P1Y2M3DT4H5M6.ABC", // Non-digit characters after decimal point
          "P1Y2M3", // Missing unit specifier
          "P1Y2M3DT", // Missing time components
          "P-T1H", // Invalid negative sign position
          "PT1H-", // Invalid negative sign position
          "P1Y2M3DT4H5M6.789123456789S", // Too many digits after decimal
          "P1Y2M3DT4H5M6.123.456S", // Multiple decimal points
          "P1Y2M3DT4H5M6,789S", // Comma instead of decimal point
        };

    for (String invalidString : invalidStrings) {
      assertThrows(SpannerException.class, () -> Interval.parseFromString(invalidString));
    }
  }

  @Test
  public void testToISO8601() {
    TestCase[] testCases =
        new TestCase[] {
          new TestCase(14, 3, 43926789000L, (short) 123, "P1Y2M3DT12H12M6.789000123S"),
          new TestCase(14, 3, 14706789000L, (short) 0, "P1Y2M3DT4H5M6.789S"),
          new TestCase(14, 3, 0, (short) 0, "P1Y2M3D"),
          new TestCase(14, 0, 0, (short) 0, "P1Y2M"),
          new TestCase(12, 0, 0, (short) 0, "P1Y"),
          new TestCase(2, 0, 0, (short) 0, "P2M"),
          new TestCase(0, 3, 0, (short) 0, "P3D"),
          new TestCase(0, 0, 15906789000L, (short) 0, "PT4H25M6.789S"),
          new TestCase(0, 0, 14430000000L, (short) 0, "PT4H30S"),
          new TestCase(0, 0, 300000000L, (short) 0, "PT5M"),
          new TestCase(0, 0, 6789000L, (short) 0, "PT6.789S"),
          new TestCase(0, 0, 123000L, (short) 0, "PT0.123S"),
          new TestCase(0, 0, 0L, (short) 123, "PT0.000000123S"),
          new TestCase(0, 0, 0, (short) 0, "P0Y"),
          new TestCase(-14, -3, -43926789000L, (short) 877, "P-1Y-2M-3DT-12H-12M-6.788999123S"),
          new TestCase(10, 3, 43746789000L, (short) 0, "P10M3DT12H9M6.789S"),
          new TestCase(-10, -3, -43866789000L, (short) 0, "P-10M-3DT-12H-11M-6.789S"),
          new TestCase(14, 3, -12906662400L, (short) 0, "P1Y2M3DT-3H-35M-6.6624S"),
          new TestCase(25, 15, 86399123456L, (short) 789, "P2Y1M15DT23H59M59.123456789S"),
          new TestCase(13, 0, 0, (short) 0, "P1Y1M"),
          new TestCase(0, 0, 86400000000L, (short) 0, "PT24H"),
          new TestCase(0, 31, 0, (short) 0, "P31D"),
          new TestCase(-12, 0, 0, (short) 0, "P-1Y"),
        };

    for (TestCase testCase : testCases) {
      Interval interval =
          Interval.builder()
              .setMonths(testCase.months)
              .setDays(testCase.days)
              .setMicroseconds(testCase.microseconds)
              .setNanoFractions(testCase.nanoFractions)
              .build();

      assertEquals(testCase.intervalString, interval.toISO8601());
    }
  }

  @Test
  public void testGetNanoseconds() {
    Interval interval1 =
        Interval.fromMonthsDaysNanos(
            10, 20, BigInteger.valueOf(30 * Interval.NANOS_PER_MICRO + 40));
    assertEquals(30 * Interval.NANOS_PER_MICRO + 40, interval1.getNanoseconds().longValueExact());

    Interval interval2 = Interval.fromMonthsDaysNanos(0, 0, BigInteger.valueOf(123456789));
    assertEquals(123456789, interval2.getNanoseconds().longValueExact());

    Interval interval3 = Interval.fromMonthsDaysNanos(-10, -20, BigInteger.valueOf(-123456789));
    assertEquals(-123456789, interval3.getNanoseconds().longValueExact());
  }

  @Test
  public void testGetAsMicroseconds() {
    Interval interval1 = Interval.fromMonthsDaysMicros(10, 20, 30);
    long expectedMicros1 = 10 * Interval.MICROS_PER_MONTH + 20 * Interval.MICROS_PER_DAY + 30;
    assertEquals(expectedMicros1, interval1.getAsMicroseconds());

    Interval interval2 = Interval.fromMonthsDaysMicros(0, 0, 123456);
    assertEquals(123456, interval2.getAsMicroseconds());

    Interval interval3 = Interval.fromMonthsDaysMicros(-10, -20, -30);
    long expectedMicros3 = -10 * Interval.MICROS_PER_MONTH - 20 * Interval.MICROS_PER_DAY - 30;
    assertEquals(expectedMicros3, interval3.getAsMicroseconds());
  }

  @Test
  public void testGetAsNanoseconds() {
    Interval interval1 = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(30));
    BigInteger expectedNanos1 =
        BigInteger.valueOf(10 * Interval.MICROS_PER_MONTH + 20 * Interval.MICROS_PER_DAY)
            .multiply(BigInteger.valueOf(Interval.NANOS_PER_MICRO))
            .add(BigInteger.valueOf(30));
    assertEquals(expectedNanos1, interval1.getAsNanoseconds());

    Interval interval2 = Interval.fromMonthsDaysNanos(0, 0, BigInteger.valueOf(123456789));
    BigInteger expectedNanos2 = BigInteger.valueOf(123456789);
    assertEquals(expectedNanos2, interval2.getAsNanoseconds());

    Interval interval3 = Interval.fromMonthsDaysNanos(-10, -20, BigInteger.valueOf(-123456789));
    BigInteger expectedNanos3 =
        BigInteger.valueOf(-10 * Interval.MICROS_PER_MONTH - 20 * Interval.MICROS_PER_DAY)
            .multiply(BigInteger.valueOf(Interval.NANOS_PER_MICRO))
            .add(BigInteger.valueOf(-123456789));
    assertEquals(expectedNanos3, interval3.getAsNanoseconds());
  }

  @Test
  public void testEquals() {
    Interval interval1 = Interval.fromMonthsDaysMicros(10, 20, 30);
    Interval interval2 = Interval.fromMonthsDaysMicros(10, 20, 30);
    Interval interval3 = Interval.fromMonthsDaysMicros(10, 20, 31);
    Interval interval4 = Interval.fromMonthsDaysMicros(10, 21, 30);
    Interval interval5 = Interval.fromMonthsDaysMicros(11, 20, 30);
    Interval interval6 = Interval.fromMonthsDaysMicros(-10, -20, -30);
    Interval interval7 = Interval.fromMonthsDaysMicros(-10, -20, -30);

    // Test with identical intervals
    assertEquals(interval1, interval2);
    assertEquals(interval2, interval1); // Check symmetry

    // Test with different intervals
    assertNotEquals(interval1, interval3);
    assertNotEquals(interval1, interval4);
    assertNotEquals(interval1, interval5);

    // Test with negative values
    assertEquals(interval6, interval7);
    assertEquals(interval7, interval6); // Check symmetry

    // Test with different values for each field (including negative)
    assertNotEquals(interval1, Interval.fromMonthsDaysMicros(1, 2, 3));
    assertNotEquals(interval1, Interval.fromMonthsDaysMicros(-10, 20, 30));
    assertNotEquals(interval1, Interval.fromMonthsDaysMicros(10, -20, 30));
    assertNotEquals(interval1, Interval.fromMonthsDaysMicros(10, 20, -30));

    // Test with nanoFractions
    Interval interval8 =
        Interval.builder()
            .setMonths(10)
            .setDays(20)
            .setMicroseconds(30)
            .setNanoFractions((short) 100)
            .build();
    Interval interval9 =
        Interval.builder()
            .setMonths(10)
            .setDays(20)
            .setMicroseconds(30)
            .setNanoFractions((short) 200)
            .build();
    assertNotEquals(interval8, interval9);

    // Test with null and an object that is not an Interval
    assertNotEquals(interval1, null);
    assertNotEquals(interval1, new Object());
  }

  @Test
  public void testHashCode() {
    Interval interval1 = Interval.fromMonthsDaysMicros(10, 20, 30);
    Interval interval2 = Interval.fromMonthsDaysMicros(10, 20, 30);
    Interval interval3 = Interval.fromMonthsDaysMicros(11, 20, 30);
    Interval interval4 = Interval.fromMonthsDaysMicros(10, 21, 30);
    Interval interval5 = Interval.fromMonthsDaysMicros(10, 20, 31);
    Interval interval6 =
        Interval.builder()
            .setMonths(10)
            .setDays(20)
            .setMicroseconds(30)
            .setNanoFractions((short) 100)
            .build();
    Interval interval7 =
        Interval.builder()
            .setMonths(10)
            .setDays(20)
            .setMicroseconds(30)
            .setNanoFractions((short) -100)
            .build();

    // Test with identical intervals
    assertEquals(interval1.hashCode(), interval2.hashCode());

    // Test with different months
    assertNotEquals(interval1.hashCode(), interval3.hashCode());

    // Test with different days
    assertNotEquals(interval1.hashCode(), interval4.hashCode());

    // Test with different microseconds
    assertNotEquals(interval1.hashCode(), interval5.hashCode());

    // Test with different nanoFractions
    assertNotEquals(interval1.hashCode(), interval6.hashCode());
    assertNotEquals(interval6.hashCode(), interval7.hashCode());

    // Test with negative values
    Interval interval8 = Interval.fromMonthsDaysMicros(-10, -20, -30);
    Interval interval9 = Interval.fromMonthsDaysMicros(-10, -20, -30);
    assertEquals(interval8.hashCode(), interval9.hashCode());
  }

  private static class TestCase {
    private final String intervalString;
    private final int months;
    private final int days;
    private final long microseconds;
    private final short nanoFractions;

    private TestCase(
        String intervalString, int months, int days, long microseconds, short nanoFractions) {
      this.intervalString = intervalString;
      this.months = months;
      this.days = days;
      this.microseconds = microseconds;
      this.nanoFractions = nanoFractions;
    }

    private TestCase(
        int months, int days, long microseconds, short nanoFractions, String intervalString) {
      this.intervalString = intervalString;
      this.months = months;
      this.days = days;
      this.microseconds = microseconds;
      this.nanoFractions = nanoFractions;
    }
  }
}
