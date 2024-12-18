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

import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the time duration as a combination of months, days and nanoseconds. Nanoseconds are
 * broken into two components microseconds and nanoFractions, where nanoFractions can range from
 * [-999, 999]. Internally, Spanner supports Interval value with the following range of individual
 * fields: months: [-120000, 120000] days: [-3660000, 3660000] nanoseconds: [-316224000000000000000,
 * 316224000000000000000] Interval value created outside the specified domain will return error when
 * sent to Spanner backend.
 */
@Immutable
public class Interval implements Serializable {
  private final int months;
  private final int days;
  private final long microseconds;
  private final short nanoFractions;

  public static final long MONTHS_PER_YEAR = 12;
  public static final long DAYS_PER_MONTH = 30;
  public static final long HOURS_PER_DAY = 24;
  public static final long MINUTES_PER_HOUR = 60;
  public static final long SECONDS_PER_MINUTE = 60;
  public static final long SECONDS_PER_HOUR = MINUTES_PER_HOUR * SECONDS_PER_MINUTE;
  public static final long MILLIS_PER_SECOND = 1000;
  public static final long MICROS_PER_MILLI = 1000;
  public static final long NANOS_PER_MICRO = 1000;
  public static final long MICROS_PER_SECOND = MICROS_PER_MILLI * MILLIS_PER_SECOND;
  public static final long MICROS_PER_MINUTE = SECONDS_PER_MINUTE * MICROS_PER_SECOND;
  public static final long MICROS_PER_HOUR = SECONDS_PER_HOUR * MICROS_PER_SECOND;
  public static final long MICROS_PER_DAY = HOURS_PER_DAY * MICROS_PER_HOUR;
  public static final long MICROS_PER_MONTH = DAYS_PER_MONTH * MICROS_PER_DAY;
  public static final BigInteger NANOS_PER_SECOND =
      BigInteger.valueOf(MICROS_PER_SECOND * NANOS_PER_MICRO);
  public static final BigInteger NANOS_PER_MINUTE =
      BigInteger.valueOf(MICROS_PER_MINUTE * NANOS_PER_MICRO);
  public static final BigInteger NANOS_PER_HOUR =
      BigInteger.valueOf(MICROS_PER_HOUR * NANOS_PER_MICRO);
  public static final Interval ZERO = Interval.builder().build();

  /** Regex to ISO8601 formatted interval. `P[n]Y[n]M[n]DT[n]H[n]M[n(.[fraction])]S` */
  private static final Pattern INTERVAL_PATTERN =
      Pattern.compile(
          "^P(?!$)(-?\\d+Y)?(-?\\d+M)?(-?\\d+D)?(T(?=-?.?\\d)(-?\\d+H)?(-?\\d+M)?(-?((\\d+(\\.\\d{1,9})?)|(\\.\\d{1,9}))S)?)?$");

  private Interval(int months, int days, long microseconds, short nanoFractions) {
    this.months = months;
    this.days = days;

    // Keep nanoFractions between [0, 1000).
    if (nanoFractions < 0) {
      nanoFractions += (short) NANOS_PER_MICRO;
      microseconds -= 1;
    }

    this.microseconds = microseconds;
    this.nanoFractions = nanoFractions;
  }

  /** Returns the months component of the interval. */
  public int getMonths() {
    return months;
  }

  /** Returns the days component of the interval. */
  public int getDays() {
    return days;
  }

  /** Returns the microseconds component of the interval. */
  public long getMicroseconds() {
    return microseconds;
  }

  /** Returns the nanoFractions component of the interval. */
  public short getNanoFractions() {
    return nanoFractions;
  }

  /** Returns the microseconds and nanoFraction of the Interval combined as nanoseconds. */
  public BigInteger getNanoseconds() {
    return BigInteger.valueOf(getMicroseconds())
        .multiply(BigInteger.valueOf(NANOS_PER_MICRO))
        .add(BigInteger.valueOf(getNanoFractions()));
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns the total microseconds represented by the interval. It combines months, days and
   * microseconds fields of the interval into microseconds.
   */
  public long getAsMicroseconds() {
    return Math.addExact(
        Math.addExact(
            Math.multiplyExact(getMonths(), MICROS_PER_MONTH),
            Math.multiplyExact(getDays(), MICROS_PER_DAY)),
        getMicroseconds());
  }

  /**
   * Returns the total nanoseconds represented by the interval. It combines months, days,
   * microseconds and nanoFractions fields of the interval into nanoseconds.
   */
  public BigInteger getAsNanoseconds() {
    return BigInteger.valueOf(getAsMicroseconds())
        .multiply(BigInteger.valueOf(NANOS_PER_MICRO))
        .add(BigInteger.valueOf(getNanoFractions()));
  }

  /** Creates an interval with specified number of months. */
  public static Interval ofMonths(int months) {
    return builder().setMonths(months).build();
  }

  /** Creates an interval with specified number of days. */
  public static Interval ofDays(int days) {
    return builder().setDays(days).build();
  }

  /** Creates an interval with specified number of seconds. */
  public static Interval ofSeconds(long seconds) {
    return builder().setMicroseconds(seconds * MICROS_PER_SECOND).build();
  }

  /** Creates an interval with specified number of milliseconds. */
  public static Interval ofMilliseconds(long milliseconds) {
    return builder().setMicroseconds(milliseconds * MICROS_PER_MILLI).build();
  }

  /** Creates an interval with specified number of microseconds. */
  public static Interval ofMicroseconds(long micros) {
    return builder().setMicroseconds(micros).build();
  }

  /** Creates an interval with specified number of nanoseconds. */
  public static Interval ofNanoseconds(@NotNull BigInteger nanos) {
    BigInteger micros = nanos.divide(BigInteger.valueOf(NANOS_PER_MICRO));
    BigInteger nanoFractions = nanos.subtract(micros.multiply(BigInteger.valueOf(NANOS_PER_MICRO)));
    long microsValue = micros.longValueExact();
    short nanoFractionsValue = nanoFractions.shortValueExact();
    return builder().setMicroseconds(microsValue).setNanoFractions(nanoFractionsValue).build();
  }

  /** Creates an interval with specified number of months, days and microseconds. */
  public static Interval fromMonthsDaysMicros(int months, int days, long micros) {
    return builder().setMonths(months).setDays(days).setMicroseconds(micros).build();
  }

  /** Creates an interval with specified number of months, days and nanoseconds. */
  public static Interval fromMonthsDaysNanos(int months, int days, BigInteger nanos) {
    long micros = (nanos.divide(BigInteger.valueOf(NANOS_PER_MICRO))).longValueExact();
    short nanoFractions =
        (nanos.subtract(BigInteger.valueOf(micros).multiply(BigInteger.valueOf(NANOS_PER_MICRO))))
            .shortValue();

    return builder()
        .setMonths(months)
        .setDays(days)
        .setMicroseconds(micros)
        .setNanoFractions(nanoFractions)
        .build();
  }

  private static String getNullOrDefault(Matcher matcher, int groupIdx) {
    String value = matcher.group(groupIdx);
    return value == null ? "0" : value;
  }

  public static Interval parseFromString(String interval) {
    Matcher matcher = INTERVAL_PATTERN.matcher(interval);
    if (!matcher.matches()) {
      throw SpannerExceptionFactory.newSpannerException(
          ErrorCode.INVALID_ARGUMENT, "Invalid Interval String: " + interval);
    }

    long years = Long.parseLong(getNullOrDefault(matcher, 1).replace("Y", ""));
    long months = Long.parseLong(getNullOrDefault(matcher, 2).replace("M", ""));
    long days = Long.parseLong(getNullOrDefault(matcher, 3).replace("D", ""));
    long hours = Long.parseLong(getNullOrDefault(matcher, 5).replace("H", ""));
    long minutes = Long.parseLong(getNullOrDefault(matcher, 6).replace("M", ""));
    BigDecimal seconds = new BigDecimal(getNullOrDefault(matcher, 7).replace("S", ""));

    long totalMonths = Math.addExact(Math.multiplyExact(years, MONTHS_PER_YEAR), months);
    BigInteger totalNanos = seconds.movePointRight(9).toBigInteger();
    totalNanos =
        totalNanos.add(BigInteger.valueOf(minutes * SECONDS_PER_MINUTE).multiply(NANOS_PER_SECOND));
    totalNanos =
        totalNanos.add(BigInteger.valueOf(hours * SECONDS_PER_HOUR).multiply(NANOS_PER_SECOND));

    BigInteger totalMicros = totalNanos.divide(BigInteger.valueOf(NANOS_PER_MICRO));
    BigInteger nanoFractions =
        totalNanos.subtract(totalMicros.multiply(BigInteger.valueOf(NANOS_PER_MICRO)));

    return Interval.builder()
        .setMonths(Math.toIntExact(totalMonths))
        .setDays(Math.toIntExact(days))
        .setMicroseconds(totalMicros.longValueExact())
        .setNanoFractions(nanoFractions.shortValueExact())
        .build();
  }

  /** Converts Interval to ISO8601 Duration Formatted String. */
  public String toISO8601() {
    if (this.equals(ZERO)) {
      return "P0Y";
    }

    StringBuilder result = new StringBuilder();
    result.append("P");

    long months_part = this.getMonths();
    long years_part = months_part / MONTHS_PER_YEAR;
    months_part = months_part - years_part * MONTHS_PER_YEAR;

    if (years_part != 0) {
      result.append(String.format("%dY", years_part));
    }

    if (months_part != 0) {
      result.append(String.format("%dM", months_part));
    }

    if (this.getDays() != 0) {
      result.append(String.format("%dD", this.getDays()));
    }

    BigInteger nanos = this.getNanoseconds();
    BigInteger zero = BigInteger.valueOf(0);
    if (nanos.compareTo(zero) != 0) {
      result.append("T");
      BigInteger hours_part = nanos.divide(NANOS_PER_HOUR);
      nanos = nanos.subtract(hours_part.multiply(NANOS_PER_HOUR));
      if (hours_part.compareTo(zero) != 0) {
        result.append(String.format("%sH", hours_part));
      }

      BigInteger minutes_part = nanos.divide(NANOS_PER_MINUTE);
      nanos = nanos.subtract(minutes_part.multiply(NANOS_PER_MINUTE));
      if (minutes_part.compareTo(zero) != 0) {
        result.append(String.format("%sM", minutes_part));
      }

      if (!nanos.equals(zero)) {
        String seconds_sign = "";
        if (nanos.signum() == -1) {
          seconds_sign = "-";
          nanos = nanos.negate();
        }

        BigInteger seconds_part = nanos.divide(NANOS_PER_SECOND);
        nanos = nanos.subtract(seconds_part.multiply(NANOS_PER_SECOND));
        result.append(String.format("%s%s", seconds_sign, seconds_part));

        if (!nanos.equals(zero)) {
          result.append(String.format(".%09d", nanos).replaceAll("0+$", ""));
        }
        result.append("S");
      }
    }

    return result.toString();
  }

  @Override
  public String toString() {
    return toISO8601();
  }

  @Override
  public boolean equals(Object rhs) {
    if (!(rhs instanceof Interval)) {
      return false;
    }

    Interval anotherInterval = (Interval) rhs;
    return getMonths() == anotherInterval.getMonths()
        && getDays() == anotherInterval.getDays()
        && getNanoseconds().equals(anotherInterval.getNanoseconds());
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + Long.valueOf(getMonths()).hashCode();
    result = 31 * result + Long.valueOf(getDays()).hashCode();
    result = 31 * result + getNanoseconds().hashCode();
    return result;
  }

  public static class Builder {
    private int months = 0;
    private int days = 0;
    private long microseconds = 0;
    private short nanoFractions = 0;

    Builder setMonths(int months) {
      this.months = months;
      return this;
    }

    Builder setDays(int days) {
      this.days = days;
      return this;
    }

    Builder setMicroseconds(long microseconds) {
      this.microseconds = microseconds;
      return this;
    }

    Builder setNanoFractions(short nanoFractions) {
      if (nanoFractions <= -NANOS_PER_MICRO || nanoFractions >= NANOS_PER_MICRO) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT,
            String.format(
                "NanoFractions must be between:[-%d, %d]",
                NANOS_PER_MICRO - 1, NANOS_PER_MICRO - 1));
      }

      this.nanoFractions = nanoFractions;
      return this;
    }

    public Interval build() {
      return new Interval(months, days, microseconds, nanoFractions);
    }
  }
}
