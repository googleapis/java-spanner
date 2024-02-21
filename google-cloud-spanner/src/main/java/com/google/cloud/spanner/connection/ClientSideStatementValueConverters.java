/*
 * Copyright 2019 Google LLC
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

package com.google.cloud.spanner.connection;

import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.Options.RpcPriority;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.TimestampBound.Mode;
import com.google.cloud.spanner.connection.PgTransactionMode.AccessMode;
import com.google.cloud.spanner.connection.PgTransactionMode.IsolationLevel;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.protobuf.Duration;
import com.google.protobuf.util.Durations;
import com.google.spanner.v1.DirectedReadOptions;
import com.google.spanner.v1.RequestOptions.Priority;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Contains all {@link ClientSideStatementValueConverter} implementations. */
class ClientSideStatementValueConverters {
  /** Map for mapping case-insensitive strings to enums. */
  private static final class CaseInsensitiveEnumMap<E extends Enum<E>> {
    private final Map<String, E> map = new HashMap<>();

    /** Create an map using the name of the enum elements as keys. */
    private CaseInsensitiveEnumMap(Class<E> elementType) {
      this(elementType, Enum::name);
    }

    /** Create a map using the specific function to get the key per enum value. */
    private CaseInsensitiveEnumMap(Class<E> elementType, Function<E, String> keyFunction) {
      Preconditions.checkNotNull(elementType);
      Preconditions.checkNotNull(keyFunction);
      EnumSet<E> set = EnumSet.allOf(elementType);
      for (E e : set) {
        if (map.put(keyFunction.apply(e).toUpperCase(), e) != null) {
          throw new IllegalArgumentException(
              "Enum contains multiple elements with the same case-insensitive key");
        }
      }
    }

    private E get(String value) {
      Preconditions.checkNotNull(value);
      return map.get(value.toUpperCase());
    }
  }

  /** Converter from string to {@link Boolean} */
  static class BooleanConverter implements ClientSideStatementValueConverter<Boolean> {

    public BooleanConverter(String allowedValues) {}

    @Override
    public Class<Boolean> getParameterClass() {
      return Boolean.class;
    }

    @Override
    public Boolean convert(String value) {
      if ("true".equalsIgnoreCase(value)) {
        return Boolean.TRUE;
      }
      if ("false".equalsIgnoreCase(value)) {
        return Boolean.FALSE;
      }
      return null;
    }
  }

  /** Converter from string to {@link Boolean} */
  static class PgBooleanConverter implements ClientSideStatementValueConverter<Boolean> {

    public PgBooleanConverter(String allowedValues) {}

    @Override
    public Class<Boolean> getParameterClass() {
      return Boolean.class;
    }

    @Override
    public Boolean convert(String value) {
      if (value == null) {
        return null;
      }
      if (value.length() > 1
          && ((value.startsWith("'") && value.endsWith("'"))
              || (value.startsWith("\"") && value.endsWith("\"")))) {
        value = value.substring(1, value.length() - 1);
      }
      if ("true".equalsIgnoreCase(value)
          || "tru".equalsIgnoreCase(value)
          || "tr".equalsIgnoreCase(value)
          || "t".equalsIgnoreCase(value)
          || "on".equalsIgnoreCase(value)
          || "1".equalsIgnoreCase(value)
          || "yes".equalsIgnoreCase(value)
          || "ye".equalsIgnoreCase(value)
          || "y".equalsIgnoreCase(value)) {
        return Boolean.TRUE;
      }
      if ("false".equalsIgnoreCase(value)
          || "fals".equalsIgnoreCase(value)
          || "fal".equalsIgnoreCase(value)
          || "fa".equalsIgnoreCase(value)
          || "f".equalsIgnoreCase(value)
          || "off".equalsIgnoreCase(value)
          || "of".equalsIgnoreCase(value)
          || "0".equalsIgnoreCase(value)
          || "no".equalsIgnoreCase(value)
          || "n".equalsIgnoreCase(value)) {
        return Boolean.FALSE;
      }
      return null;
    }
  }

  /** Converter from string to a non-negative integer. */
  static class NonNegativeIntegerConverter implements ClientSideStatementValueConverter<Integer> {

    public NonNegativeIntegerConverter(String allowedValues) {}

    @Override
    public Class<Integer> getParameterClass() {
      return Integer.class;
    }

    @Override
    public Integer convert(String value) {
      try {
        int res = Integer.parseInt(value);
        if (res < 0) {
          // The conventions for these converters is to return null if the value is invalid.
          return null;
        }
        return res;
      } catch (Exception ignore) {
        return null;
      }
    }
  }

  /** Converter from string to {@link Duration}. */
  static class DurationConverter implements ClientSideStatementValueConverter<Duration> {
    private final Pattern allowedValues;

    public DurationConverter(String allowedValues) {
      // Remove the parentheses from the beginning and end.
      this.allowedValues =
          Pattern.compile(
              "(?is)\\A" + allowedValues.substring(1, allowedValues.length() - 1) + "\\z");
    }

    @Override
    public Class<Duration> getParameterClass() {
      return Duration.class;
    }

    @Override
    public Duration convert(String value) {
      Matcher matcher = allowedValues.matcher(value);
      if (matcher.find()) {
        if (matcher.group(0).equalsIgnoreCase("null")) {
          return Durations.fromNanos(0L);
        } else {
          Duration duration =
              ReadOnlyStalenessUtil.createDuration(
                  Long.parseLong(matcher.group(1)),
                  ReadOnlyStalenessUtil.parseTimeUnit(matcher.group(2)));
          if (duration.getSeconds() == 0L && duration.getNanos() == 0) {
            return null;
          }
          return duration;
        }
      }
      return null;
    }
  }

  /** Converter from string to {@link Duration}. */
  static class PgDurationConverter implements ClientSideStatementValueConverter<Duration> {
    private final Pattern allowedValues;

    public PgDurationConverter(String allowedValues) {
      // Remove the parentheses from the beginning and end.
      this.allowedValues =
          Pattern.compile(
              "(?is)\\A" + allowedValues.substring(1, allowedValues.length() - 1) + "\\z");
    }

    @Override
    public Class<Duration> getParameterClass() {
      return Duration.class;
    }

    @Override
    public Duration convert(String value) {
      Matcher matcher = allowedValues.matcher(value);
      if (matcher.find()) {
        Duration duration;
        if (matcher.group(0).equalsIgnoreCase("default")) {
          return Durations.fromNanos(0L);
        } else if (matcher.group(2) == null) {
          duration =
              ReadOnlyStalenessUtil.createDuration(
                  Long.parseLong(matcher.group(0)), TimeUnit.MILLISECONDS);
        } else {
          duration =
              ReadOnlyStalenessUtil.createDuration(
                  Long.parseLong(matcher.group(1)),
                  ReadOnlyStalenessUtil.parseTimeUnit(matcher.group(2)));
        }
        if (duration.getSeconds() == 0L && duration.getNanos() == 0) {
          return null;
        }
        return duration;
      }
      return null;
    }
  }

  /** Converter from string to possible values for read only staleness ({@link TimestampBound}). */
  static class ReadOnlyStalenessConverter
      implements ClientSideStatementValueConverter<TimestampBound> {
    private final Pattern allowedValues;
    private final CaseInsensitiveEnumMap<Mode> values = new CaseInsensitiveEnumMap<>(Mode.class);

    public ReadOnlyStalenessConverter(String allowedValues) {
      // Remove the single quotes at the beginning and end.
      this.allowedValues =
          Pattern.compile(
              "(?is)\\A" + allowedValues.substring(1, allowedValues.length() - 1) + "\\z");
    }

    @Override
    public Class<TimestampBound> getParameterClass() {
      return TimestampBound.class;
    }

    @Override
    public TimestampBound convert(String value) {
      Matcher matcher = allowedValues.matcher(value);
      if (matcher.find() && matcher.groupCount() >= 1) {
        Mode mode = null;
        int groupIndex = 0;
        for (int group = 1; group <= matcher.groupCount(); group++) {
          if (matcher.group(group) != null) {
            mode = values.get(matcher.group(group));
            if (mode != null) {
              groupIndex = group;
              break;
            }
          }
        }
        switch (mode) {
          case STRONG:
            return TimestampBound.strong();
          case READ_TIMESTAMP:
            return TimestampBound.ofReadTimestamp(
                ReadOnlyStalenessUtil.parseRfc3339(matcher.group(groupIndex + 1)));
          case MIN_READ_TIMESTAMP:
            return TimestampBound.ofMinReadTimestamp(
                ReadOnlyStalenessUtil.parseRfc3339(matcher.group(groupIndex + 1)));
          case EXACT_STALENESS:
            try {
              return TimestampBound.ofExactStaleness(
                  Long.parseLong(matcher.group(groupIndex + 2)),
                  ReadOnlyStalenessUtil.parseTimeUnit(matcher.group(groupIndex + 3)));
            } catch (IllegalArgumentException e) {
              throw SpannerExceptionFactory.newSpannerException(
                  ErrorCode.INVALID_ARGUMENT, e.getMessage());
            }
          case MAX_STALENESS:
            try {
              return TimestampBound.ofMaxStaleness(
                  Long.parseLong(matcher.group(groupIndex + 2)),
                  ReadOnlyStalenessUtil.parseTimeUnit(matcher.group(groupIndex + 3)));
            } catch (IllegalArgumentException e) {
              throw SpannerExceptionFactory.newSpannerException(
                  ErrorCode.INVALID_ARGUMENT, e.getMessage());
            }
          default:
            // fall through to allow the calling method to handle this
        }
      }
      return null;
    }
  }
  /**
   * Converter from string to possible values for {@link com.google.spanner.v1.DirectedReadOptions}.
   */
  static class DirectedReadOptionsConverter
      implements ClientSideStatementValueConverter<DirectedReadOptions> {
    private final Pattern allowedValues;

    public DirectedReadOptionsConverter(String allowedValues) {
      // Remove the single quotes at the beginning and end.
      this.allowedValues =
          Pattern.compile(
              "(?is)\\A" + allowedValues.substring(1, allowedValues.length() - 1) + "\\z");
    }

    @Override
    public Class<DirectedReadOptions> getParameterClass() {
      return DirectedReadOptions.class;
    }

    @Override
    public DirectedReadOptions convert(String value) {
      Matcher matcher = allowedValues.matcher(value);
      if (matcher.find()) {
        try {
          return DirectedReadOptionsUtil.parse(value);
        } catch (SpannerException spannerException) {
          throw SpannerExceptionFactory.newSpannerException(
              ErrorCode.INVALID_ARGUMENT,
              String.format(
                  "Failed to parse '%s' as a valid value for DIRECTED_READ.\n"
                      + "The value should be a JSON string like this: '%s'.\n"
                      + "You can generate a valid JSON string from a DirectedReadOptions instance by calling %s.%s",
                  value,
                  "{\"includeReplicas\":{\"replicaSelections\":[{\"location\":\"eu-west1\",\"type\":\"READ_ONLY\"}]}}",
                  DirectedReadOptionsUtil.class.getName(),
                  "toString(DirectedReadOptions directedReadOptions)"),
              spannerException);
        }
      }
      return null;
    }
  }

  /** Converter for converting strings to {@link AutocommitDmlMode} values. */
  static class AutocommitDmlModeConverter
      implements ClientSideStatementValueConverter<AutocommitDmlMode> {
    private final CaseInsensitiveEnumMap<AutocommitDmlMode> values =
        new CaseInsensitiveEnumMap<>(AutocommitDmlMode.class);

    public AutocommitDmlModeConverter(String allowedValues) {}

    @Override
    public Class<AutocommitDmlMode> getParameterClass() {
      return AutocommitDmlMode.class;
    }

    @Override
    public AutocommitDmlMode convert(String value) {
      return values.get(value);
    }
  }

  static class StringValueConverter implements ClientSideStatementValueConverter<String> {
    public StringValueConverter(String allowedValues) {}

    @Override
    public Class<String> getParameterClass() {
      return String.class;
    }

    @Override
    public String convert(String value) {
      return value;
    }
  }

  /** Converter for converting string values to {@link TransactionMode} values. */
  static class TransactionModeConverter
      implements ClientSideStatementValueConverter<TransactionMode> {
    private final CaseInsensitiveEnumMap<TransactionMode> values =
        new CaseInsensitiveEnumMap<>(TransactionMode.class, TransactionMode::getStatementString);

    public TransactionModeConverter(String allowedValues) {}

    @Override
    public Class<TransactionMode> getParameterClass() {
      return TransactionMode.class;
    }

    @Override
    public TransactionMode convert(String value) {
      // Transaction mode may contain multiple spaces.
      String valueWithSingleSpaces = value.replaceAll("\\s+", " ");
      return values.get(valueWithSingleSpaces);
    }
  }

  static class PgTransactionIsolationConverter
      implements ClientSideStatementValueConverter<IsolationLevel> {
    private final CaseInsensitiveEnumMap<IsolationLevel> values =
        new CaseInsensitiveEnumMap<>(IsolationLevel.class, IsolationLevel::getShortStatementString);

    public PgTransactionIsolationConverter(String allowedValues) {}

    @Override
    public Class<IsolationLevel> getParameterClass() {
      return IsolationLevel.class;
    }

    @Override
    public IsolationLevel convert(String value) {
      // Isolation level may contain multiple spaces.
      String valueWithSingleSpaces = value.replaceAll("\\s+", " ");
      if (valueWithSingleSpaces.length() > 1
          && ((valueWithSingleSpaces.startsWith("'") && valueWithSingleSpaces.endsWith("'"))
              || (valueWithSingleSpaces.startsWith("\"")
                  && valueWithSingleSpaces.endsWith("\"")))) {
        valueWithSingleSpaces =
            valueWithSingleSpaces.substring(1, valueWithSingleSpaces.length() - 1);
      }
      return values.get(valueWithSingleSpaces);
    }
  }

  /**
   * Converter for converting string values to {@link PgTransactionMode} values. Includes no-op
   * handling of setting the isolation level of the transaction to default or serializable.
   */
  static class PgTransactionModeConverter
      implements ClientSideStatementValueConverter<PgTransactionMode> {
    PgTransactionModeConverter() {}

    public PgTransactionModeConverter(String allowedValues) {}

    @Override
    public Class<PgTransactionMode> getParameterClass() {
      return PgTransactionMode.class;
    }

    @Override
    public PgTransactionMode convert(String value) {
      PgTransactionMode mode = new PgTransactionMode();
      // Transaction mode may contain multiple spaces.
      String valueWithoutDeferrable = value.replaceAll("(?i)(not\\s+deferrable)", " ");
      String valueWithSingleSpaces =
          valueWithoutDeferrable.replaceAll("\\s+", " ").toLowerCase(Locale.ENGLISH).trim();
      int currentIndex = 0;
      while (currentIndex < valueWithSingleSpaces.length()) {
        // This will use the last access mode and isolation level that is encountered in the string.
        // This is consistent with the behavior of PostgreSQL, which also allows multiple modes to
        // be specified in one string, and will use the last one that is encountered.
        if (valueWithSingleSpaces.substring(currentIndex).startsWith("read only")) {
          currentIndex += "read only".length();
          mode.setAccessMode(AccessMode.READ_ONLY_TRANSACTION);
        } else if (valueWithSingleSpaces.substring(currentIndex).startsWith("read write")) {
          currentIndex += "read write".length();
          mode.setAccessMode(AccessMode.READ_WRITE_TRANSACTION);
        } else if (valueWithSingleSpaces
            .substring(currentIndex)
            .startsWith("isolation level serializable")) {
          currentIndex += "isolation level serializable".length();
          mode.setIsolationLevel(IsolationLevel.ISOLATION_LEVEL_SERIALIZABLE);
        } else if (valueWithSingleSpaces
            .substring(currentIndex)
            .startsWith("isolation level default")) {
          currentIndex += "isolation level default".length();
          mode.setIsolationLevel(IsolationLevel.ISOLATION_LEVEL_DEFAULT);
        } else {
          return null;
        }
        // Skip space and/or comma that may separate multiple transaction modes.
        if (currentIndex < valueWithSingleSpaces.length()
            && valueWithSingleSpaces.charAt(currentIndex) == ' ') {
          currentIndex++;
        }
        if (currentIndex < valueWithSingleSpaces.length()
            && valueWithSingleSpaces.charAt(currentIndex) == ',') {
          currentIndex++;
        }
        if (currentIndex < valueWithSingleSpaces.length()
            && valueWithSingleSpaces.charAt(currentIndex) == ' ') {
          currentIndex++;
        }
      }
      return mode;
    }
  }

  /** Converter for converting strings to {@link RpcPriority} values. */
  static class RpcPriorityConverter implements ClientSideStatementValueConverter<Priority> {
    private final CaseInsensitiveEnumMap<Priority> values =
        new CaseInsensitiveEnumMap<>(Priority.class);
    private final Pattern allowedValues;

    public RpcPriorityConverter(String allowedValues) {
      // Remove the parentheses from the beginning and end.
      this.allowedValues =
          Pattern.compile(
              "(?is)\\A" + allowedValues.substring(1, allowedValues.length() - 1) + "\\z");
    }

    @Override
    public Class<Priority> getParameterClass() {
      return Priority.class;
    }

    @Override
    public Priority convert(String value) {
      Matcher matcher = allowedValues.matcher(value);
      if (matcher.find()) {
        if (matcher.group(0).equalsIgnoreCase("null")) {
          return Priority.PRIORITY_UNSPECIFIED;
        }
      }
      return values.get("PRIORITY_" + value);
    }
  }

  /** Converter for converting strings to {@link SavepointSupport} values. */
  static class SavepointSupportConverter
      implements ClientSideStatementValueConverter<SavepointSupport> {
    private final CaseInsensitiveEnumMap<SavepointSupport> values =
        new CaseInsensitiveEnumMap<>(SavepointSupport.class);

    public SavepointSupportConverter(String allowedValues) {}

    @Override
    public Class<SavepointSupport> getParameterClass() {
      return SavepointSupport.class;
    }

    @Override
    public SavepointSupport convert(String value) {
      return values.get(value);
    }
  }

  static class ExplainCommandConverter implements ClientSideStatementValueConverter<String> {
    @Override
    public Class<String> getParameterClass() {
      return String.class;
    }

    @Override
    public String convert(String value) {
      /* The first word in the string should be "explain"
       *  So, if the size of the string <= 7 (number of letters in the word "explain"), its an invalid statement
       *  If the size is greater than 7, we'll consider everything after explain as the query.
       */
      if (value.length() <= 7) {
        return null;
      }
      return value.substring(7).trim();
    }
  }
}
