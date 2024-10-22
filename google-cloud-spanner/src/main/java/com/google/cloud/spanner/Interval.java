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

import com.google.auto.value.AutoValue;
import com.google.errorprone.annotations.Immutable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

@AutoValue
@Immutable
public abstract class Interval implements Serializable {
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

  private static final Pattern INTERVAL_PATTERN =
      Pattern.compile(
          "^P(?!$)(-?\\d+Y)?(-?\\d+M)?(-?\\d+D)?(T(?=-?\\d)(-?\\d+H)?(-?\\d+M)?(-?\\d+(\\.\\d{1,9})?S)?)?$");

  /** Returns the months component of the interval. */
  public abstract int months();

  /** Returns the days component of the interval. */
  public abstract int days();

  /** Returns the microseconds component of the interval. */
  public abstract long micros();

  /** Returns the nanoFractions component of the interval. */
  public abstract short nanoFractions();

  public static Builder builder() {
    return new AutoValue_Interval.Builder();
  }

  /** Returns the nanoseconds component of the interval. */
  public BigInteger nanos() {
    return BigInteger.valueOf(micros())
        .multiply(BigInteger.valueOf(NANOS_PER_MICRO))
        .add(BigInteger.valueOf(nanoFractions()));
  }

  /** Returns the total microseconds represented by the interval. */
  public long getAsMicros() {
    return months() * MICROS_PER_MONTH + days() * MICROS_PER_DAY + micros();
  }

  /** Returns the total nanoseconds represented by the interval. */
  public BigInteger getAsNanos() {
    return BigInteger.valueOf(getAsMicros())
        .multiply(BigInteger.valueOf(NANOS_PER_MICRO))
        .add(BigInteger.valueOf(nanoFractions()));
  }

  /** Creates an interval with specified number of months. */
  public static Interval ofMonths(int months) {
    return builder().setMonths(months).setDays(0).setMicros(0).setNanoFractions((short) 0).build();
  }

  /** Creates an interval with specified number of days. */
  public static Interval ofDays(int days) {
    return builder().setMonths(0).setDays(days).setMicros(0).setNanoFractions((short) 0).build();
  }

  /** Creates an interval with specified number of seconds. */
  public static Interval ofSeconds(long seconds) {
    return builder()
        .setMonths(0)
        .setDays(0)
        .setMicros(seconds * MICROS_PER_SECOND)
        .setNanoFractions((short) 0)
        .build();
  }

  /** Creates an interval with specified number of milliseconds. */
  public static Interval ofMilliseconds(long milliseconds) {
    return builder()
        .setMonths(0)
        .setDays(0)
        .setMicros(milliseconds * MICROS_PER_MILLI)
        .setNanoFractions((short) 0)
        .build();
  }

  /** Creates an interval with specified number of microseconds. */
  public static Interval ofMicros(long micros) {
    return builder().months(0).days(0).micros(micros).nanoFractions((short) 0).build();
  }

  /** Creates an interval with specified number of nanoseconds. */
  public static Interval ofNanos(@NotNull BigInteger nanos) {
    BigInteger micros = nanos.divide(BigInteger.valueOf(NANOS_PER_MICRO));
    BigInteger nanoFractions = nanos.subtract(micros.multiply(BigInteger.valueOf(NANOS_PER_MICRO)));
    long microsValue = micros.longValueExact();
    short nanoFractionsValue = nanoFractions.shortValueExact();
    return builder()
        .setMonths(0)
        .setDays(0)
        .setMicros(microsValue)
        .setNanoFractions(nanoFractionsValue)
        .build();
  }

  /** Creates an interval with specified number of months, days and microseconds. */
  public static Interval fromMonthsDaysMicros(int months, int days, long micros) {
    return builder()
        .setMonths(months)
        .setDays(days)
        .setMicros(micros)
        .setNanoFractions((short) 0)
        .build();
  }

  /** Creates an interval with specified number of months, days and nanoseconds. */
  public static Interval fromMonthsDaysNanos(int months, int days, BigInteger nanos) {
    long micros = (nanos.divide(BigInteger.valueOf(NANOS_PER_MICRO))).longValueExact();
    short nanoFractions =
        (nanos.subtract(BigInteger.valueOf(micros).multiply(BigInteger.valueOf(NANOS_PER_MICRO))))
            .shortValue();

    System.out.println("Micros: " + micros + " Nanos: " + nanoFractions);

    return builder()
        .setMonths(months)
        .setDays(days)
        .setMicros(micros)
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

    long totalMonths = years * MONTHS_PER_YEAR + months;
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
        .setMicros(totalMicros.longValueExact())
        .setNanoFractions(nanoFractions.shortValueExact())
        .build();
  }

  /** Converts Interval to ISO8601 Duration Formatted String. */
  public String ToISO8601() {
    StringBuilder result = new StringBuilder();
    result.append("P");

    long months = this.months();
    long years = months / MONTHS_PER_YEAR;
    months = months - years * MONTHS_PER_YEAR;

    if (years != 0) {
      result.append(String.format("%dY", years));
    }

    if (months != 0) {
      result.append(String.format("%dM", months));
    }

    if (this.days() != 0) {
      result.append(String.format("%dD", this.days()));
    }

    BigInteger nanos = this.nanos();
    BigInteger zero = BigInteger.valueOf(0);
    if (nanos.compareTo(zero) != 0) {
      result.append("T");
      BigInteger hours = nanos.divide(NANOS_PER_HOUR);

      if (hours.compareTo(zero) != 0) {
        result.append(String.format("%sH", hours));
      }

      nanos = nanos.subtract(hours.multiply(NANOS_PER_HOUR));
      BigInteger minutes = nanos.divide(NANOS_PER_MINUTE);
      if (minutes.compareTo(zero) != 0) {
        result.append(String.format("%sM", minutes));
      }

      nanos = nanos.subtract(minutes.multiply(NANOS_PER_MINUTE));
      BigDecimal seconds = new BigDecimal(nanos).movePointLeft(9);

      if (seconds.compareTo(new BigDecimal(zero)) != 0) {
        result.append(String.format("%sS", seconds.stripTrailingZeros()));
      }
    }

    if (result.length() == 1) {
      result.append("0Y");
    }

    return result.toString();
  }

  /** Creates an interval which representing 0-duration. */
  public static Interval zeroInterval() {
    return builder().setMonths(0).setDays(0).setMicros(0).setNanoFractions((short) 0).build();
  }

  @Override
  public boolean equals(Object rhs) {
    if (!(rhs instanceof Interval)) {
      return false;
    }

    Interval anotherInterval = (Interval) rhs;
    return months() == anotherInterval.months()
        && days() == anotherInterval.days()
        && nanos().equals(anotherInterval.nanos());
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + Long.valueOf(months()).hashCode();
    result = 31 * result + Long.valueOf(days()).hashCode();
    result = 31 * result + nanos().hashCode();
    return result;
  }

  @AutoValue.Builder
  public abstract static class Builder {
    abstract Builder months(int months);

    abstract Builder days(int days);

    abstract Builder micros(long micros);

    abstract Builder nanoFractions(short nanoFractions);

    public Builder setMonths(int months) {
      return months(months);
    }

    public Builder setDays(int days) {
      return days(days);
    }

    public Builder setMicros(long micros) {
      return micros(micros);
    }

    public Builder setNanoFractions(short nanoFractions) {
      if (nanoFractions <= -NANOS_PER_MICRO || nanoFractions >= NANOS_PER_MICRO) {
        throw SpannerExceptionFactory.newSpannerException(
            ErrorCode.INVALID_ARGUMENT,
            String.format(
                "NanoFractions must be between:[-%d, %d]",
                NANOS_PER_MICRO - 1, NANOS_PER_MICRO - 1));
      }
      return nanoFractions(nanoFractions);
    }

    public abstract Interval build();
  }
}
