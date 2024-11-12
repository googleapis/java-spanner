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
  public void testOfGetMonths() {
    Interval interval = Interval.ofMonths(10);
    assertEquals(10, interval.getMonths());
    assertEquals(0, interval.getDays());
    assertEquals(0, interval.getMicroseconds());
    assertEquals(0, interval.getNanoFractions());
  }

  @Test
  public void testOfGetDays() {
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
  public void testOfGetMicroseconds() {
    Interval interval = Interval.ofMicroseconds(10);
    assertEquals(0, interval.getMonths());
    assertEquals(0, interval.getDays());
    assertEquals(10, interval.getMicroseconds());
    assertEquals(0, interval.getNanoFractions());
  }

  @Test
  public void testOfGetNanoseconds() {
    Interval interval = Interval.ofNanoseconds(BigInteger.valueOf(10));
    assertEquals(0, interval.getMonths());
    assertEquals(0, interval.getDays());
    assertEquals(0, interval.getMicroseconds());
    assertEquals(10, interval.getNanoFractions());
  }

  @Test
  public void testFromGetMonthsGetDaysGetMicroseconds() {
    Interval interval = Interval.fromMonthsDaysMicros(10, 20, 30);
    assertEquals(10, interval.getMonths());
    assertEquals(20, interval.getDays());
    assertEquals(30, interval.getMicroseconds());
    assertEquals(0, interval.getNanoFractions());
  }

  @Test
  public void testFromGetMonthsGetDaysGetNanoseconds() {
    Interval interval = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(1030));
    assertEquals(10, interval.getMonths());
    assertEquals(20, interval.getDays());
    assertEquals(1, interval.getMicroseconds());
    assertEquals(30, interval.getNanoFractions());

    Interval interval2 = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(-1030));
    assertEquals(10, interval2.getMonths());
    assertEquals(20, interval2.getDays());
    assertEquals(-1, interval2.getMicroseconds());
    assertEquals(-30, interval2.getNanoFractions());
  }

  @Test
  public void testParseFromString() {
    Interval interval = Interval.parseFromString("P1Y2M3DT4H5M6.789000123S");
    assertEquals(14, interval.getMonths());
    assertEquals(3, interval.getDays());
    assertEquals(
        4 * Interval.MICROS_PER_HOUR
            + 5 * Interval.MICROS_PER_MINUTE
            + 6 * Interval.MICROS_PER_SECOND
            + 789000,
        interval.getMicroseconds());
    assertEquals(123, interval.getNanoFractions());
  }

  @Test
  public void testParseFromString_InvalidString() {
    assertThrows(SpannerException.class, () -> Interval.parseFromString("invalid"));
    assertThrows(SpannerException.class, () -> Interval.parseFromString("P"));
    assertThrows(SpannerException.class, () -> Interval.parseFromString("P0"));
    assertThrows(SpannerException.class, () -> Interval.parseFromString("P0"));
    assertThrows(SpannerException.class, () -> Interval.parseFromString("PT0.12345678912S"));
    assertThrows(SpannerException.class, () -> Interval.parseFromString("PT.12345678912S"));
    assertThrows(SpannerException.class, () -> Interval.parseFromString("PT1S1S"));
    assertThrows(SpannerException.class, () -> Interval.parseFromString("P1Y2M3M"));
  }

  @Test
  public void testToISO8601() {
    Interval interval =
        Interval.fromMonthsDaysMicros(
            14,
            3,
            4 * Interval.MICROS_PER_HOUR
                + 5 * Interval.MICROS_PER_MINUTE
                + 6 * Interval.MICROS_PER_SECOND
                + 789000);
    assertEquals("P1Y2M3DT4H5M6.789S", interval.toISO8601());
  }

  @Test
  public void testToISO8601WithGetNanoseconds() {
    Interval interval =
        Interval.fromMonthsDaysNanos(
            14,
            3,
            BigInteger.valueOf(4)
                .multiply(Interval.NANOS_PER_HOUR)
                .add(BigInteger.valueOf(5).multiply(Interval.NANOS_PER_MINUTE))
                .add(BigInteger.valueOf(6).multiply(Interval.NANOS_PER_SECOND))
                .add(BigInteger.valueOf(789123456)));
    assertEquals("P1Y2M3DT4H5M6.789123456S", interval.toISO8601());
  }

  @Test
  public void testToISO8601WithOnlyGetNanoseconds() {
    Interval interval = Interval.ofNanoseconds(BigInteger.valueOf(123456789));
    assertEquals("PT0.123456789S", interval.toISO8601());
  }

  @Test
  public void testEquals() {
    Interval interval1 = Interval.fromMonthsDaysMicros(10, 20, 30);
    Interval interval2 = Interval.fromMonthsDaysMicros(10, 20, 30);
    assertEquals(interval1, interval2);
  }

  @Test
  public void testHashCode_equalObjects() {
    Interval interval1 = Interval.fromMonthsDaysMicros(10, 20, 30);
    Interval interval2 = Interval.fromMonthsDaysMicros(10, 20, 30);
    assertEquals(interval1.hashCode(), interval2.hashCode());
  }

  @Test
  public void testHashCode_differentMonths() {
    Interval interval1 = Interval.fromMonthsDaysMicros(10, 20, 30);
    Interval interval2 = Interval.fromMonthsDaysMicros(11, 20, 30);
    assertNotEquals(interval1.hashCode(), interval2.hashCode());
  }

  @Test
  public void testHashCode_differentDays() {
    Interval interval1 = Interval.fromMonthsDaysMicros(10, 20, 30);
    Interval interval2 = Interval.fromMonthsDaysMicros(10, 21, 30);
    assertNotEquals(interval1.hashCode(), interval2.hashCode());
  }

  @Test
  public void testHashCode_differentMicros() {
    Interval interval1 = Interval.fromMonthsDaysMicros(10, 20, 30);
    Interval interval2 = Interval.fromMonthsDaysMicros(10, 20, 31);
    assertNotEquals(interval1.hashCode(), interval2.hashCode());
  }

  @Test
  public void testHashCode_differentNanoFractions() {
    Interval interval1 =
        Interval.builder()
            .setMonths(10)
            .setDays(20)
            .setMicroseconds(30)
            .setNanoFractions((short) 10)
            .build();
    Interval interval2 =
        Interval.builder()
            .setMonths(10)
            .setDays(20)
            .setMicroseconds(30)
            .setNanoFractions((short) -10)
            .build();
    assertNotEquals(interval1.hashCode(), interval2.hashCode());
  }

  @Test
  public void testGetAsGetMicroseconds() {
    Interval interval = Interval.fromMonthsDaysMicros(10, 20, 30);
    long expectedMicros = 10 * Interval.MICROS_PER_MONTH + 20 * Interval.MICROS_PER_DAY + 30;
    assertEquals(expectedMicros, interval.getAsMicroseconds());
  }

  @Test
  public void testGetAsGetNanoseconds() {
    Interval interval = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(30));
    BigInteger expectedNanos =
        BigInteger.valueOf(10 * Interval.MICROS_PER_MONTH + 20 * Interval.MICROS_PER_DAY)
            .multiply(BigInteger.valueOf(Interval.NANOS_PER_MICRO))
            .add(BigInteger.valueOf(30));
    assertEquals(expectedNanos, interval.getAsNanoseconds());
  }

  @Test
  public void testGetNanoseconds() {
    Interval interval =
        Interval.fromMonthsDaysNanos(
            10, 20, BigInteger.valueOf(30 * Interval.NANOS_PER_MICRO + 40));
    assertEquals(30 * Interval.NANOS_PER_MICRO + 40, interval.getNanoseconds().longValueExact());
  }

  @Test
  public void testParseFromStringWithGetNanoseconds() {
    Interval interval = Interval.parseFromString("P1Y2M3DT4H5M6.789123456S");
    assertEquals(14, interval.getMonths());
    assertEquals(3, interval.getDays());
    assertEquals(
        4 * Interval.MICROS_PER_HOUR
            + 5 * Interval.MICROS_PER_MINUTE
            + 6 * Interval.MICROS_PER_SECOND
            + 789123,
        interval.getMicroseconds());
    assertEquals(456, interval.getNanoFractions());
  }

  @Test
  public void testParseFromStringWithOnlyGetNanoseconds() {
    Interval interval = Interval.parseFromString("PT.123456789S");
    assertEquals(0, interval.getMonths());
    assertEquals(0, interval.getDays());
    assertEquals(123456, interval.getMicroseconds());
    assertEquals(789, interval.getNanoFractions());
  }

  @Test
  public void testParseFromStringWithZeroes() {
    Interval interval = Interval.parseFromString("P0Y0M0DT0H0M0.0S");
    assertEquals(0, interval.getMonths());
    assertEquals(0, interval.getDays());
    assertEquals(0, interval.getMicroseconds());
    assertEquals(0, interval.getNanoFractions());
  }

  @Test
  public void testToISO8601WithZeroes() {
    Interval interval = Interval.ZERO;
    assertEquals("P0Y", interval.toISO8601());
  }

  @Test
  public void testParseFromStringWithNegativeValues() {
    Interval interval = Interval.parseFromString("P-1Y-2M-3DT-4H-5M-6.789S");
    assertEquals(-14, interval.getMonths());
    assertEquals(-3, interval.getDays());
    assertEquals(
        -4 * Interval.MICROS_PER_HOUR
            - 5 * Interval.MICROS_PER_MINUTE
            - 6 * Interval.MICROS_PER_SECOND
            - 789000,
        interval.getMicroseconds());
    assertEquals(0, interval.getNanoFractions());
  }

  @Test
  public void testToISO8601WithNegativeValues() {
    Interval interval =
        Interval.fromMonthsDaysMicros(
            -14,
            -3,
            -4 * Interval.MICROS_PER_HOUR
                - 5 * Interval.MICROS_PER_MINUTE
                - 6 * Interval.MICROS_PER_SECOND
                - 789000);
    assertEquals("P-1Y-2M-3DT-4H-5M-6.789S", interval.toISO8601());
  }

  @Test
  public void testEqualsWithGetNanoseconds() {
    Interval interval1 = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(30));
    Interval interval2 = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(30));
    assertEquals(interval1, interval2);
  }
}
