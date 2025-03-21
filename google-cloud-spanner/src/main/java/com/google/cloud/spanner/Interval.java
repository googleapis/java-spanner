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
 * 316224000000000000000]. Interval value created outside the specified domain will return error
 * when sent to Spanner backend.
 */
@Immutable
public class Interval implements Serializable {
  private final int months;
  private final int days;
  private final BigInteger nanoseconds;

  public static final long MONTHS_PER_YEAR = 12;
  public static final long MINUTES_PER_HOUR = 60;
  public static final long SECONDS_PER_MINUTE = 60;
  public static final long SECONDS_PER_HOUR = MINUTES_PER_HOUR * SECONDS_PER_MINUTE;
  public static final long MILLIS_PER_SECOND = 1000;
  public static final long MICROS_PER_MILLISECOND = 1000;
  public static final long MICROS_PER_SECOND = MICROS_PER_MILLISECOND * MILLIS_PER_SECOND;
  public static final long MICROS_PER_MINUTE = SECONDS_PER_MINUTE * MICROS_PER_SECOND;
  public static final long MICROS_PER_HOUR = SECONDS_PER_HOUR * MICROS_PER_SECOND;
  public static final long NANOS_PER_MICROSECOND = 1000;
  public static final BigInteger NANOS_PER_MILLISECOND =
      BigInteger.valueOf(MICROS_PER_MILLISECOND * NANOS_PER_MICROSECOND);
  public static final BigInteger NANOS_PER_SECOND =
      BigInteger.valueOf(MICROS_PER_SECOND * NANOS_PER_MICROSECOND);
  public static final BigInteger NANOS_PER_MINUTE =
      BigInteger.valueOf(MICROS_PER_MINUTE * NANOS_PER_MICROSECOND);
  public static final BigInteger NANOS_PER_HOUR =
      BigInteger.valueOf(MICROS_PER_HOUR * NANOS_PER_MICROSECOND);
  public static final Interval ZERO = Interval.builder().build();

  /** Regex to parse ISO8601 interval format- `P[n]Y[n]M[n]DT[n]H[n]M[n([.,][fraction])]S` */
  private static final Pattern INTERVAL_PATTERN =
      Pattern.compile(
          "^P(?!$)(-?\\d+Y)?(-?\\d+M)?(-?\\d+D)?(T(?=-?[.,]?\\d)(-?\\d+H)?(-?\\d+M)?(-?((\\d+([.,]\\d{1,9})?)|([.,]\\d{1,9}))S)?)?$");

  private Interval(int months, int days, BigInteger nanoseconds) {
    this.months = months;
    this.days = days;
    this.nanoseconds = nanoseconds;
  }

  /** Returns the months component of the interval. */
  public int getMonths() {
    return months;
  }

  /** Returns the days component of the interval. */
  public int getDays() {
    return days;
  }

  /** Returns the nanoseconds component of the interval. */
  public BigInteger getNanoseconds() {
    return nanoseconds;
  }

  public static Builder builder() {
    return new Builder();
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
    return builder().setNanoseconds(BigInteger.valueOf(seconds).multiply(NANOS_PER_SECOND)).build();
  }

  /** Creates an interval with specified number of milliseconds. */
  public static Interval ofMilliseconds(long milliseconds) {
    return builder()
        .setNanoseconds(BigInteger.valueOf(milliseconds).multiply(NANOS_PER_MILLISECOND))
        .build();
  }

  /** Creates an interval with specified number of microseconds. */
  public static Interval ofMicroseconds(long micros) {
    return builder()
        .setNanoseconds(
            BigInteger.valueOf(micros).multiply(BigInteger.valueOf(NANOS_PER_MICROSECOND)))
        .build();
  }

  /** Creates an interval with specified number of nanoseconds. */
  public static Interval ofNanoseconds(@NotNull BigInteger nanos) {
    return builder().setNanoseconds(nanos).build();
  }

  /** Creates an interval with specified number of months, days and nanoseconds. */
  public static Interval fromMonthsDaysNanos(int months, int days, BigInteger nanoseconds) {
    return builder().setMonths(months).setDays(days).setNanoseconds(nanoseconds).build();
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
    BigDecimal seconds =
        new BigDecimal(getNullOrDefault(matcher, 7).replace("S", "").replace(",", "."));

    long totalMonths = Math.addExact(Math.multiplyExact(years, MONTHS_PER_YEAR), months);
    BigInteger totalNanos = seconds.movePointRight(9).toBigInteger();
    totalNanos =
        totalNanos.add(BigInteger.valueOf(minutes * SECONDS_PER_MINUTE).multiply(NANOS_PER_SECOND));
    totalNanos =
        totalNanos.add(BigInteger.valueOf(hours * SECONDS_PER_HOUR).multiply(NANOS_PER_SECOND));

    return Interval.builder()
        .setMonths(Math.toIntExact(totalMonths))
        .setDays(Math.toIntExact(days))
        .setNanoseconds(totalNanos)
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
          result.append(String.format(".%09d", nanos).replaceAll("(0{3})+$", ""));
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
    result = 31 * result + Integer.valueOf(getMonths()).hashCode();
    result = 31 * result + Integer.valueOf(getDays()).hashCode();
    result = 31 * result + getNanoseconds().hashCode();
    return result;
  }

  public static class Builder {
    private int months = 0;
    private int days = 0;
    private BigInteger nanoseconds = BigInteger.ZERO;

    Builder setMonths(int months) {
      this.months = months;
      return this;
    }

    Builder setDays(int days) {
      this.days = days;
      return this;
    }

    Builder setNanoseconds(BigInteger nanoseconds) {
      this.nanoseconds = nanoseconds;
      return this;
    }

    public Interval build() {
      return new Interval(months, days, nanoseconds);
    }
  }
}
