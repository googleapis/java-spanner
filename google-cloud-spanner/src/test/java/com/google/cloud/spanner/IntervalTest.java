/*
 * Copyright 2021 Google LLC
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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
    assertEquals(10, interval.months());
    assertEquals(0, interval.days());
    assertEquals(0, interval.micros());
    assertEquals(0, interval.nanoFractions());
  }

  @Test
  public void testOfDays() {
    Interval interval = Interval.ofDays(10);
    assertEquals(0, interval.months());
    assertEquals(10, interval.days());
    assertEquals(0, interval.micros());
    assertEquals(0, interval.nanoFractions());
  }

  @Test
  public void testOfSeconds() {
    Interval interval = Interval.ofSeconds(10);
    assertEquals(0, interval.months());
    assertEquals(0, interval.days());
    assertEquals(10 * Interval.MICROS_PER_SECOND, interval.micros());
    assertEquals(0, interval.nanoFractions());
  }

  @Test
  public void testOfMilliseconds() {
    Interval interval = Interval.ofMilliseconds(10);
    assertEquals(0, interval.months());
    assertEquals(0, interval.days());
    assertEquals(10 * Interval.MICROS_PER_MILLI, interval.micros());
    assertEquals(0, interval.nanoFractions());
  }

  @Test
  public void testOfMicros() {
    Interval interval = Interval.ofMicros(10);
    assertEquals(0, interval.months());
    assertEquals(0, interval.days());
    assertEquals(10, interval.micros());
    assertEquals(0, interval.nanoFractions());
  }

  @Test
  public void testOfNanos() {
    Interval interval = Interval.ofNanos(BigInteger.valueOf(10));
    assertEquals(0, interval.months());
    assertEquals(0, interval.days());
    assertEquals(0, interval.micros());
    assertEquals(10, interval.nanoFractions());
  }

  @Test
  public void testFromMonthsDaysMicros() {
    Interval interval = Interval.fromMonthsDaysMicros(10, 20, 30);
    assertEquals(10, interval.months());
    assertEquals(20, interval.days());
    assertEquals(30, interval.micros());
    assertEquals(0, interval.nanoFractions());
  }

  @Test
  public void testFromMonthsDaysNanos() {
    Interval interval = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(1030));
    assertEquals(10, interval.months());
    assertEquals(20, interval.days());
    assertEquals(1, interval.micros());
    assertEquals(30, interval.nanoFractions());

    Interval interval2 = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(-1030));
    assertEquals(10, interval2.months());
    assertEquals(20, interval2.days());
    assertEquals(-1, interval2.micros());
    assertEquals(-30, interval2.nanoFractions());
  }

  @Test
  public void testParseFromString() {
    Interval interval = Interval.parseFromString("P1Y2M3DT4H5M6.789000123S");
    assertEquals(14, interval.months());
    assertEquals(3, interval.days());
    assertEquals(
        4 * Interval.MICROS_PER_HOUR
            + 5 * Interval.MICROS_PER_MINUTE
            + 6 * Interval.MICROS_PER_SECOND
            + 789000,
        interval.micros());
    assertEquals(123, interval.nanoFractions());
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
    assertEquals("P1Y2M3DT4H5M6.789S", interval.ToISO8601());
  }

  @Test
  public void testToISO8601_ZeroInterval() {
    Interval interval = Interval.zeroInterval();
    assertEquals("P0Y", interval.ToISO8601());
  }

  @Test
  public void testToISO8601WithNanos() {
    Interval interval =
        Interval.fromMonthsDaysNanos(
            14,
            3,
            BigInteger.valueOf(4)
                .multiply(Interval.NANOS_PER_HOUR)
                .add(BigInteger.valueOf(5).multiply(Interval.NANOS_PER_MINUTE))
                .add(BigInteger.valueOf(6).multiply(Interval.NANOS_PER_SECOND))
                .add(BigInteger.valueOf(789123456)));
    assertEquals("P1Y2M3DT4H5M6.789123456S", interval.ToISO8601());
  }

  @Test
  public void testToISO8601WithOnlyNanos() {
    Interval interval = Interval.ofNanos(BigInteger.valueOf(123456789));
    assertEquals("PT0.123456789S", interval.ToISO8601());
  }

  @Test
  public void testEquals() {
    Interval interval1 = Interval.fromMonthsDaysMicros(10, 20, 30);
    Interval interval2 = Interval.fromMonthsDaysMicros(10, 20, 30);
    assertEquals(interval1, interval2);
  }

  @Test
  public void testHashCode() {
    Interval interval1 = Interval.fromMonthsDaysMicros(10, 20, 30);
    Interval interval2 = Interval.fromMonthsDaysMicros(10, 20, 30);
    assertEquals(interval1.hashCode(), interval2.hashCode());
  }

  @Test
  public void testGetAsMicros() {
    Interval interval = Interval.fromMonthsDaysMicros(10, 20, 30);
    long expectedMicros = 10 * Interval.MICROS_PER_MONTH + 20 * Interval.MICROS_PER_DAY + 30;
    assertEquals(expectedMicros, interval.getAsMicros());
  }

  @Test
  public void testGetAsNanos() {
    Interval interval = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(30));
    BigInteger expectedNanos =
        BigInteger.valueOf(10 * Interval.MICROS_PER_MONTH + 20 * Interval.MICROS_PER_DAY)
            .multiply(BigInteger.valueOf(Interval.NANOS_PER_MICRO))
            .add(BigInteger.valueOf(30));
    assertEquals(expectedNanos, interval.getAsNanos());
  }

  @Test
  public void testNanos() {
    Interval interval =
        Interval.fromMonthsDaysNanos(
            10, 20, BigInteger.valueOf(30 * Interval.NANOS_PER_MICRO + 40));
    assertEquals(30 * Interval.NANOS_PER_MICRO + 40, interval.nanos().longValueExact());
  }

  @Test
  public void testParseFromStringWithNanos() {
    Interval interval = Interval.parseFromString("P1Y2M3DT4H5M6.789123456S");
    assertEquals(14, interval.months());
    assertEquals(3, interval.days());
    assertEquals(
        4 * Interval.MICROS_PER_HOUR
            + 5 * Interval.MICROS_PER_MINUTE
            + 6 * Interval.MICROS_PER_SECOND
            + 789123,
        interval.micros());
    assertEquals(456, interval.nanoFractions());
  }

  @Test
  public void testParseFromStringWithOnlyNanos() {
    Interval interval = Interval.parseFromString("PT.123456789S");
    assertEquals(0, interval.months());
    assertEquals(0, interval.days());
    assertEquals(123456, interval.micros());
    assertEquals(789, interval.nanoFractions());
  }

  @Test
  public void testParseFromStringWithZeroes() {
    Interval interval = Interval.parseFromString("P0Y0M0DT0H0M0.0S");
    assertEquals(0, interval.months());
    assertEquals(0, interval.days());
    assertEquals(0, interval.micros());
    assertEquals(0, interval.nanoFractions());
  }

  @Test
  public void testToISO8601WithZeroes() {
    Interval interval = Interval.zeroInterval();
    assertEquals("P0Y", interval.ToISO8601());
  }

  @Test
  public void testParseFromStringWithNegativeValues() {
    Interval interval = Interval.parseFromString("P-1Y-2M-3DT-4H-5M-6.789S");
    assertEquals(-14, interval.months());
    assertEquals(-3, interval.days());
    assertEquals(
        -4 * Interval.MICROS_PER_HOUR
            - 5 * Interval.MICROS_PER_MINUTE
            - 6 * Interval.MICROS_PER_SECOND
            - 789000,
        interval.micros());
    assertEquals(0, interval.nanoFractions());
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
    assertEquals("P-1Y-2M-3DT-4H-5M-6.789S", interval.ToISO8601());
  }

  @Test
  public void testEqualsWithNanos() {
    Interval interval1 = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(30));
    Interval interval2 = Interval.fromMonthsDaysNanos(10, 20, BigInteger.valueOf(30));
    assertEquals(interval1, interval2);
  }
}
