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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.math.BigInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IntervalTest {

  @Test
  public void testConstructionAndInitialization() {
    Interval interval1 =
        Interval.builder().setMonths(1).setDays(2).setMicros(3).setNanoFractions((short) 4).build();
    assertThat(interval1.months()).isEqualTo(1);
    assertThat(interval1.days()).isEqualTo(2);
    assertThat(interval1.micros()).isEqualTo(3);
    assertThat(interval1.nanoFractions()).isEqualTo((short) 4);

    Interval interval2 = Interval.ofMonths(5);
    assertThat(interval2.months()).isEqualTo(5);
    assertThat(interval2.days()).isEqualTo(0);
    assertThat(interval2.micros()).isEqualTo(0);
    assertThat(interval2.nanoFractions()).isEqualTo((short) 0);

    Interval interval3 = Interval.ofDays(10);
    assertThat(interval3.months()).isEqualTo(0);
    assertThat(interval3.days()).isEqualTo(10);
    assertThat(interval3.micros()).isEqualTo(0);
    assertThat(interval3.nanoFractions()).isEqualTo((short) 0);

    Interval interval4 = Interval.fromMonthsDaysMicros(2, 5, 1000);
    assertThat(interval4.months()).isEqualTo(2);
    assertThat(interval4.days()).isEqualTo(5);
    assertThat(interval4.micros()).isEqualTo(1000);
    assertThat(interval4.nanoFractions()).isEqualTo((short) 0);

    Interval interval5 = Interval.fromMonthsDaysNanos(3, 10, BigInteger.valueOf(5000));
    assertThat(interval5.months()).isEqualTo(3);
    assertThat(interval5.days()).isEqualTo(10);
    assertThat(interval5.micros()).isEqualTo(5);
    assertThat(interval5.nanoFractions()).isEqualTo((short) 0);
  }

  @Test
  public void testUnitConversions() {
    Interval interval =
        Interval.builder().setMonths(1).setDays(2).setMicros(3).setNanoFractions((short) 4).build();
    assertThat(interval.nanos())
        .isEqualTo(
            BigInteger.valueOf(3)
                .multiply(BigInteger.valueOf(Interval.NANOS_PER_MICRO))
                .add(BigInteger.valueOf(4)));
    assertThat(interval.getAsMicros())
        .isEqualTo(1 * Interval.MICROS_PER_MONTH + 2 * Interval.MICROS_PER_DAY + 3);
    assertThat(interval.getAsNanos())
        .isEqualTo(
            BigInteger.valueOf(interval.getAsMicros())
                .multiply(BigInteger.valueOf(Interval.NANOS_PER_MICRO))
                .add(BigInteger.valueOf(4)));
  }

  @Test
  public void testParsingAndFormatting() {
    Interval interval1 = Interval.parseFromString("P1Y2M3DT4H5M6.78912345S");
    assertThat(interval1.months()).isEqualTo(14);
    assertThat(interval1.days()).isEqualTo(3);
    assertThat(interval1.micros())
        .isEqualTo(4 * Interval.MICROS_PER_HOUR + 5 * Interval.MICROS_PER_MINUTE + 6789123);
    assertThat(interval1.nanoFractions()).isEqualTo((short) 450);
    assertThat(interval1.ToISO8601()).isEqualTo("P1Y2M3DT4H5M6.789123450S");

    Interval interval2 = Interval.parseFromString("P1Y");
    assertThat(interval2.months()).isEqualTo(12);
    assertThat(interval2.days()).isEqualTo(0);
    assertThat(interval2.micros()).isEqualTo(0);
    assertThat(interval2.nanoFractions()).isEqualTo((short) 0);
    assertThat(interval2.ToISO8601()).isEqualTo("P1Y");

    Interval interval3 = Interval.parseFromString("P0Y");
    assertThat(interval3.months()).isEqualTo(0);
    assertThat(interval3.days()).isEqualTo(0);
    assertThat(interval3.micros()).isEqualTo(0);
    assertThat(interval3.nanoFractions()).isEqualTo((short) 0);
    assertThat(interval3.ToISO8601()).isEqualTo("P0Y");

    assertThrows(SpannerException.class, () -> Interval.parseFromString("invalid"));
    assertThrows(SpannerException.class, () -> Interval.parseFromString("P"));
    assertThrows(SpannerException.class, () -> Interval.parseFromString("P0"));
    assertThrows(SpannerException.class, () -> Interval.parseFromString("P1M1M"));
    assertThrows(SpannerException.class, () -> Interval.parseFromString("PT0"));
    assertThrows(SpannerException.class, () -> Interval.parseFromString("PT0.1234567890S"));
  }

  @Test
  public void testComparisonAndEquality() {
    Interval interval1 = Interval.fromMonthsDaysMicros(1, 2, 3);
    Interval interval2 = Interval.fromMonthsDaysMicros(1, 2, 3);
    Interval interval3 = Interval.fromMonthsDaysMicros(2, 2, 3);

    assertThat(interval1.compareTo(interval2)).isEqualTo(0);
    assertThat(interval1.compareTo(interval3)).isLessThan(0);
    assertThat(interval3.compareTo(interval1)).isGreaterThan(0);
  }

  @Test
  public void testOfSeconds() {
    Interval interval = Interval.ofSeconds(10);
    assertThat(interval.getAsMicros()).isEqualTo(10 * Interval.MICROS_PER_SECOND);
  }

  @Test
  public void testOfMilliseconds() {
    Interval interval = Interval.ofMilliseconds(200);
    assertThat(interval.getAsMicros()).isEqualTo(200 * Interval.MICROS_PER_MILLI);
  }

  @Test
  public void testOfMicros() {
    Interval interval = Interval.ofMicros(3000000);
    assertThat(interval.getAsMicros()).isEqualTo(3000000);
  }

  @Test
  public void testOfNanos() {
    Interval interval = Interval.ofNanos(BigInteger.valueOf(4000000000L));
    assertThat(interval.getAsNanos()).isEqualTo(BigInteger.valueOf(4000000000L));
  }

  @Test
  public void testZeroInterval() {
    Interval interval = Interval.zeroInterval();
    assertThat(interval.getAsNanos()).isEqualTo(BigInteger.ZERO);
  }

  @Test
  public void testBuilder_invalidNanoFractions() {
    assertThrows(
        SpannerException.class, () -> Interval.builder().setNanoFractions((short) -1000).build());
    assertThrows(
        SpannerException.class, () -> Interval.builder().setNanoFractions((short) 1000).build());
  }
}
