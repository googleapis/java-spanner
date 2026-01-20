/*
 * Copyright 2026 Google LLC
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

package com.google.cloud.spanner.spi.v1;

import com.google.api.core.InternalApi;
import com.google.protobuf.ByteString;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.spanner.v1.KeyRange;
import com.google.spanner.v1.KeySet;
import com.google.spanner.v1.Mutation;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@InternalApi
public final class KeyRecipe {

  // kInfinity is "\xff" - the largest single byte, used as a sentinel for ranges
  private static final ByteString K_INFINITY = ByteString.copyFrom(new byte[] {(byte) 0xFF});

  private enum Kind {
    TAG,
    VALUE,
    INVALID
  }

  private enum KeyType {
    FULL_KEY,
    PREFIX,
    PREFIX_SUCCESSOR,
    INDEX_KEY
  }

  private enum EncodeState {
    OK,
    FAILED,
    END_OF_KEYS
  }

  private static final class Part {
    private final Kind kind;
    private final int tag; // if kind == TAG
    private final com.google.spanner.v1.Type type; // if kind == VALUE
    private final com.google.spanner.v1.KeyRecipe.Part.Order order; // if kind == VALUE
    private final com.google.spanner.v1.KeyRecipe.Part.NullOrder nullOrder; // if kind == VALUE
    private final String identifier; // if kind == VALUE
    private final List<Integer> structIdentifiers; // if kind == VALUE
    private final Value constantValue; // if kind == VALUE and value is set
    private final boolean random; // if kind == VALUE and random: true

    private Value constantValue() {
      return constantValue;
    }

    private boolean hasConstantValue() {
      return constantValue != null;
    }

    private Part(
        Kind kind,
        int tag,
        com.google.spanner.v1.Type type,
        com.google.spanner.v1.KeyRecipe.Part.Order order,
        com.google.spanner.v1.KeyRecipe.Part.NullOrder nullOrder,
        String identifier,
        List<Integer> structIdentifiers,
        Value constantValue,
        boolean random) {
      this.kind = kind;
      this.tag = tag;
      this.type = type;
      this.order = order;
      this.nullOrder = nullOrder;
      this.identifier = identifier;
      this.structIdentifiers = structIdentifiers;
      this.constantValue = constantValue;
      this.random = random;
    }

    private ResolvedValue resolveValue(BiFunction<Integer, String, Value> valueFinder, int index) {
      if (hasConstantValue()) {
        return ResolvedValue.ofValue(constantValue());
      }
      Value value = valueFinder.apply(index, identifier == null ? "" : identifier);
      if (value == null) {
        return ResolvedValue.missing();
      }
      if (structIdentifiers.isEmpty()) {
        return ResolvedValue.ofValue(value);
      }
      Value current = value;
      for (int fieldIndex : structIdentifiers) {
        if (current.getKindCase() != Value.KindCase.LIST_VALUE
            || fieldIndex < 0
            || fieldIndex >= current.getListValue().getValuesCount()) {
          return ResolvedValue.failed();
        }
        current = current.getListValue().getValues(fieldIndex);
      }
      return ResolvedValue.ofValue(current);
    }

    private boolean shouldConsumeValueIndex() {
      return !hasConstantValue() && !random;
    }

    static Part fromProto(com.google.spanner.v1.KeyRecipe.Part partProto) {
      if (partProto.getTag() > 0) {
        return new Part(Kind.TAG, partProto.getTag(), null, null, null, null, null, null, false);
      }
      if (!partProto.hasType()) {
        return new Part(Kind.INVALID, 0, null, null, null, null, null, null, false);
      }
      if (partProto.getOrder() != com.google.spanner.v1.KeyRecipe.Part.Order.ASCENDING
          && partProto.getOrder() != com.google.spanner.v1.KeyRecipe.Part.Order.DESCENDING) {
        return new Part(Kind.INVALID, 0, null, null, null, null, null, null, false);
      }
      if (partProto.getNullOrder() != com.google.spanner.v1.KeyRecipe.Part.NullOrder.NULLS_FIRST
          && partProto.getNullOrder() != com.google.spanner.v1.KeyRecipe.Part.NullOrder.NULLS_LAST
          && partProto.getNullOrder() != com.google.spanner.v1.KeyRecipe.Part.NullOrder.NOT_NULL) {
        return new Part(Kind.INVALID, 0, null, null, null, null, null, null, false);
      }
      if (partProto.hasRandom()
          && partProto.getType().getCode() != com.google.spanner.v1.TypeCode.INT64) {
        return new Part(Kind.INVALID, 0, null, null, null, null, null, null, false);
      }

      String identifier = partProto.hasIdentifier() ? partProto.getIdentifier() : null;
      List<Integer> structIdentifiers = new ArrayList<>();
      for (int fieldIndex : partProto.getStructIdentifiersList()) {
        structIdentifiers.add(fieldIndex);
      }

      Value constantValue = partProto.hasValue() ? partProto.getValue() : null;

      return new Part(
          Kind.VALUE,
          0,
          partProto.getType(),
          partProto.getOrder(),
          partProto.getNullOrder(),
          identifier,
          structIdentifiers,
          constantValue,
          partProto.hasRandom());
    }
  }

  private static void encodeRandomValuePart(Part part, UnsynchronizedByteArrayOutputStream out) {
    long value = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
    boolean ascending = part.order == com.google.spanner.v1.KeyRecipe.Part.Order.ASCENDING;
    if (ascending) {
      SsFormat.appendInt64Increasing(out, value);
    } else {
      SsFormat.appendInt64Decreasing(out, value);
    }
  }

  private static final class ResolvedValue {
    private final Value value;
    private final boolean found;
    private final boolean failed;

    private ResolvedValue(Value value, boolean found, boolean failed) {
      this.value = value;
      this.found = found;
      this.failed = failed;
    }

    private static ResolvedValue ofValue(Value value) {
      return new ResolvedValue(value, true, false);
    }

    private static ResolvedValue missing() {
      return new ResolvedValue(null, false, false);
    }

    private static ResolvedValue failed() {
      return new ResolvedValue(null, false, true);
    }
  }

  private final List<Part> parts;
  private final int numValueParts;
  private final boolean isIndex;

  private KeyRecipe(List<Part> parts, int numValueParts, boolean isIndex) {
    this.parts = parts;
    this.numValueParts = numValueParts;
    this.isIndex = isIndex;
  }

  public static KeyRecipe create(com.google.spanner.v1.KeyRecipe in) {
    List<Part> partsList = new ArrayList<>();
    int valuePartsCount = 0;
    boolean isIndex = in.hasIndexName();
    for (com.google.spanner.v1.KeyRecipe.Part partProto : in.getPartList()) {
      Part part = Part.fromProto(partProto);
      partsList.add(part);
      if (part.kind == Kind.VALUE) {
        valuePartsCount++;
      }
    }
    if (partsList.isEmpty()) {
      throw new IllegalArgumentException("KeyRecipe must have at least one part.");
    }
    if (partsList.get(0).kind != Kind.TAG) {
      throw new IllegalArgumentException("KeyRecipe must start with a tag.");
    }
    return new KeyRecipe(partsList, valuePartsCount, isIndex);
  }

  private static void encodeNull(Part part, UnsynchronizedByteArrayOutputStream out) {
    switch (part.nullOrder) {
      case NULLS_FIRST:
        SsFormat.appendNullOrderedFirst(out);
        break;
      case NULLS_LAST:
        SsFormat.appendNullOrderedLast(out);
        break;
      case NOT_NULL:
        throw new IllegalArgumentException("Key part cannot be NULL");
      default:
        throw new IllegalArgumentException("Unknown null order: " + part.nullOrder);
    }
  }

  private static void encodeNotNull(Part part, UnsynchronizedByteArrayOutputStream out) {
    switch (part.nullOrder) {
      case NULLS_FIRST:
        SsFormat.appendNotNullMarkerNullOrderedFirst(out);
        break;
      case NULLS_LAST:
        SsFormat.appendNotNullMarkerNullOrderedLast(out);
        break;
      case NOT_NULL:
        // No marker needed for NOT_NULL
        break;
      default:
        throw new IllegalArgumentException("Unknown null order: " + part.nullOrder);
    }
  }

  private static void encodeSingleValuePart(
      Part part, Value value, UnsynchronizedByteArrayOutputStream out) {
    if (value.getKindCase() == Value.KindCase.NULL_VALUE) {
      encodeNull(part, out);
      return;
    }

    // Validate type compatibility BEFORE encoding anything
    validateValueType(part, value);

    // Now safe to encode the NOT_NULL marker
    encodeNotNull(part, out);

    boolean isAscending = (part.order == com.google.spanner.v1.KeyRecipe.Part.Order.ASCENDING);

    switch (part.type.getCode()) {
      case BOOL:
        if (isAscending) {
          SsFormat.appendBoolIncreasing(out, value.getBoolValue());
        } else {
          SsFormat.appendBoolDecreasing(out, value.getBoolValue());
        }
        break;
      case INT64:
        long intVal = Long.parseLong(value.getStringValue());
        if (isAscending) {
          SsFormat.appendInt64Increasing(out, intVal);
        } else {
          SsFormat.appendInt64Decreasing(out, intVal);
        }
        break;
      case FLOAT64:
        if (value.getKindCase() == Value.KindCase.STRING_VALUE) {
          // Handle special float values like Infinity, -Infinity, NaN
          String strVal = value.getStringValue();
          double dblVal;
          if ("Infinity".equals(strVal)) {
            dblVal = Double.POSITIVE_INFINITY;
          } else if ("-Infinity".equals(strVal)) {
            dblVal = Double.NEGATIVE_INFINITY;
          } else if ("NaN".equals(strVal)) {
            dblVal = Double.NaN;
          } else {
            throw new IllegalArgumentException("Invalid FLOAT64 string: " + strVal);
          }
          if (isAscending) {
            SsFormat.appendDoubleIncreasing(out, dblVal);
          } else {
            SsFormat.appendDoubleDecreasing(out, dblVal);
          }
        } else {
          if (isAscending) {
            SsFormat.appendDoubleIncreasing(out, value.getNumberValue());
          } else {
            SsFormat.appendDoubleDecreasing(out, value.getNumberValue());
          }
        }
        break;
      case STRING:
        if (isAscending) {
          SsFormat.appendStringIncreasing(out, value.getStringValue());
        } else {
          SsFormat.appendStringDecreasing(out, value.getStringValue());
        }
        break;
      case BYTES:
        byte[] bytesDecoded = Base64.getDecoder().decode(value.getStringValue());
        if (isAscending) {
          SsFormat.appendBytesIncreasing(out, bytesDecoded);
        } else {
          SsFormat.appendBytesDecreasing(out, bytesDecoded);
        }
        break;
      case TIMESTAMP:
        {
          String tsStr = value.getStringValue();
          long[] parsed = parseTimestamp(tsStr);
          byte[] encoded = SsFormat.encodeTimestamp(parsed[0], (int) parsed[1]);
          if (isAscending) {
            SsFormat.appendBytesIncreasing(out, encoded);
          } else {
            SsFormat.appendBytesDecreasing(out, encoded);
          }
        }
        break;
      case DATE:
        {
          String dateStr = value.getStringValue();
          int daysSinceEpoch = parseDate(dateStr);
          if (isAscending) {
            SsFormat.appendInt64Increasing(out, daysSinceEpoch);
          } else {
            SsFormat.appendInt64Decreasing(out, daysSinceEpoch);
          }
        }
        break;
      case UUID:
        {
          String uuidStr = value.getStringValue();
          long[] parsed = parseUuid(uuidStr);
          byte[] encoded = SsFormat.encodeUuid(parsed[0], parsed[1]);
          if (isAscending) {
            SsFormat.appendBytesIncreasing(out, encoded);
          } else {
            SsFormat.appendBytesDecreasing(out, encoded);
          }
        }
        break;
      case ENUM:
        // ENUM values are sent as string representation of the enum number
        long enumVal = Long.parseLong(value.getStringValue());
        if (isAscending) {
          SsFormat.appendInt64Increasing(out, enumVal);
        } else {
          SsFormat.appendInt64Decreasing(out, enumVal);
        }
        break;
      case NUMERIC:
      case TYPE_CODE_UNSPECIFIED:
      case ARRAY:
      case STRUCT:
      case PROTO:
      case UNRECOGNIZED:
      default:
        throw new IllegalArgumentException(
            "Unsupported type code for ssformat encoding: " + part.type.getCode());
    }
  }

  private static void validateValueType(Part part, Value value) {
    switch (part.type.getCode()) {
      case BOOL:
        if (value.getKindCase() != Value.KindCase.BOOL_VALUE) {
          throw new IllegalArgumentException("Type mismatch for BOOL.");
        }
        break;
      case INT64:
        if (value.getKindCase() != Value.KindCase.STRING_VALUE) {
          throw new IllegalArgumentException("Type mismatch for INT64, expecting decimal string.");
        }
        // Also validate it's a valid integer
        try {
          Long.parseLong(value.getStringValue());
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Invalid INT64 string: " + value.getStringValue(), e);
        }
        break;
      case FLOAT64:
        if (value.getKindCase() != Value.KindCase.NUMBER_VALUE
            && value.getKindCase() != Value.KindCase.STRING_VALUE) {
          throw new IllegalArgumentException("Type mismatch for FLOAT64.");
        }
        if (value.getKindCase() == Value.KindCase.STRING_VALUE) {
          String strVal = value.getStringValue();
          if (!"Infinity".equals(strVal) && !"-Infinity".equals(strVal) && !"NaN".equals(strVal)) {
            throw new IllegalArgumentException("Invalid FLOAT64 string: " + strVal);
          }
        }
        break;
      case STRING:
        if (value.getKindCase() != Value.KindCase.STRING_VALUE) {
          throw new IllegalArgumentException("Type mismatch for STRING.");
        }
        break;
      case BYTES:
        if (value.getKindCase() != Value.KindCase.STRING_VALUE) {
          throw new IllegalArgumentException("Type mismatch for BYTES, expecting base64 string.");
        }
        // Validate base64
        try {
          Base64.getDecoder().decode(value.getStringValue());
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("Invalid base64 for BYTES type.", e);
        }
        break;
      case TIMESTAMP:
        if (value.getKindCase() != Value.KindCase.STRING_VALUE) {
          throw new IllegalArgumentException("Type mismatch for TIMESTAMP.");
        }
        // Validate timestamp format: must end with Z (UTC) and be RFC3339
        validateTimestamp(value.getStringValue());
        break;
      case DATE:
        if (value.getKindCase() != Value.KindCase.STRING_VALUE) {
          throw new IllegalArgumentException("Type mismatch for DATE.");
        }
        // Validate date format: YYYY-MM-DD, exactly 10 chars
        validateDate(value.getStringValue());
        break;
      case UUID:
        if (value.getKindCase() != Value.KindCase.STRING_VALUE) {
          throw new IllegalArgumentException("Type mismatch for UUID.");
        }
        // Validate UUID format
        validateUuid(value.getStringValue());
        break;
      case ENUM:
        if (value.getKindCase() != Value.KindCase.STRING_VALUE) {
          throw new IllegalArgumentException("Type mismatch for ENUM, expecting string.");
        }
        // Validate it's a valid integer string
        try {
          Long.parseLong(value.getStringValue());
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(
              "Invalid ENUM string (expecting number): " + value.getStringValue(), e);
        }
        break;
      case NUMERIC:
      case TYPE_CODE_UNSPECIFIED:
      case ARRAY:
      case STRUCT:
      case PROTO:
      case UNRECOGNIZED:
      default:
        throw new IllegalArgumentException(
            "Unsupported type code for ssformat encoding: " + part.type.getCode());
    }
  }

  // RFC3339 timestamp pattern: YYYY-MM-DDTHH:MM:SS[.nnnnnnnnn]Z
  // Allow any number of decimal places (will be truncated to 9)
  private static final Pattern TIMESTAMP_PATTERN =
      Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})(\\.\\d+)?Z$");

  private static void validateTimestamp(String ts) {
    if (!ts.endsWith("Z")) {
      throw new IllegalArgumentException("Invalid TIMESTAMP string: " + ts);
    }
    Matcher m = TIMESTAMP_PATTERN.matcher(ts);
    if (!m.matches()) {
      throw new IllegalArgumentException("Invalid TIMESTAMP string: " + ts);
    }
    // Validate ranges
    int year = Integer.parseInt(m.group(1));
    int month = Integer.parseInt(m.group(2));
    int day = Integer.parseInt(m.group(3));
    int hour = Integer.parseInt(m.group(4));
    int minute = Integer.parseInt(m.group(5));
    int second = Integer.parseInt(m.group(6));
    if (month < 1 || month > 12 || day < 1 || day > 31 || hour > 23 || minute > 59 || second > 59) {
      throw new IllegalArgumentException("Invalid TIMESTAMP string: " + ts);
    }
    // Year must be 0000-9999 (year 0 is allowed)
    if (year < 0 || year > 9999) {
      throw new IllegalArgumentException("Invalid TIMESTAMP string: " + ts);
    }
  }

  private static long[] parseTimestamp(String ts) {
    // Parse RFC3339 timestamp using Java time library
    // Remove trailing Z and parse
    String withoutZ = ts.substring(0, ts.length() - 1);

    // Parse date-time parts
    int dotIdx = withoutZ.indexOf('.');
    String dateTimePart;
    int nanos = 0;
    if (dotIdx >= 0) {
      dateTimePart = withoutZ.substring(0, dotIdx);
      String fracStr = withoutZ.substring(dotIdx + 1);
      // Pad to 9 digits
      while (fracStr.length() < 9) {
        fracStr = fracStr + "0";
      }
      // Truncate to 9 digits
      if (fracStr.length() > 9) {
        fracStr = fracStr.substring(0, 9);
      }
      nanos = Integer.parseInt(fracStr);
    } else {
      dateTimePart = withoutZ;
    }

    // Parse date and time components
    // Format: YYYY-MM-DDTHH:MM:SS
    String[] dateTime = dateTimePart.split("T");
    String[] dateParts = dateTime[0].split("-");
    String[] timeParts = dateTime[1].split(":");

    int year = Integer.parseInt(dateParts[0]);
    int month = Integer.parseInt(dateParts[1]);
    int day = Integer.parseInt(dateParts[2]);
    int hour = Integer.parseInt(timeParts[0]);
    int minute = Integer.parseInt(timeParts[1]);
    int second = Integer.parseInt(timeParts[2]);

    // Compute days since epoch using proleptic Gregorian calendar
    long days = civilDayNumber(year, month, day);
    long seconds = days * 86400L + hour * 3600L + minute * 60L + second;

    return new long[] {seconds, nanos};
  }

  // Compute the civil day number (days since Unix epoch 1970-01-01)
  // This matches absl::CivilDay calculation
  private static long civilDayNumber(int year, int month, int day) {
    // Algorithm from http://howardhinnant.github.io/date_algorithms.html
    // This produces the same results as absl::CivilDay
    int y = year;
    int m = month;
    int d = day;

    // Adjust year and month (March = month 1 in this algorithm)
    if (m <= 2) {
      y -= 1;
      m += 12;
    }
    m -= 3;

    // Days from era 0 (year 0 March 1) to given date
    int era = (y >= 0 ? y : y - 399) / 400;
    int yoe = y - era * 400; // year of era [0, 399]
    int doy = (153 * m + 2) / 5 + d - 1; // day of year [0, 365]
    int doe = yoe * 365 + yoe / 4 - yoe / 100 + doy; // day of era [0, 146096]
    long dayNumber =
        (long) era * 146097 + doe - 719468; // shift epoch from 0000-03-01 to 1970-01-01

    return dayNumber;
  }

  private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");

  private static void validateDate(String dateStr) {
    if (dateStr.length() != 10) {
      throw new IllegalArgumentException("Invalid DATE string: " + dateStr);
    }
    Matcher m = DATE_PATTERN.matcher(dateStr);
    if (!m.matches()) {
      throw new IllegalArgumentException("Invalid DATE string: " + dateStr);
    }
    int year = Integer.parseInt(m.group(1));
    int month = Integer.parseInt(m.group(2));
    int day = Integer.parseInt(m.group(3));
    if (month < 1 || month > 12 || day < 1 || day > 31) {
      throw new IllegalArgumentException("Invalid DATE string: " + dateStr);
    }
    // Year can be 0000-9999 for DATE
    if (year < 0 || year > 9999) {
      throw new IllegalArgumentException("Invalid DATE string: " + dateStr);
    }
  }

  private static int parseDate(String dateStr) {
    Matcher m = DATE_PATTERN.matcher(dateStr);
    if (!m.matches()) {
      throw new IllegalArgumentException("Invalid DATE string: " + dateStr);
    }
    int year = Integer.parseInt(m.group(1));
    int month = Integer.parseInt(m.group(2));
    int day = Integer.parseInt(m.group(3));
    return (int) civilDayNumber(year, month, day);
  }

  private static void validateUuid(String uuid) {
    long[] result = parseUuid(uuid);
    // parseUuid throws if invalid
  }

  private static final int K_UUID_LENGTH = 36;

  private static long[] parseUuid(String uuid) {
    String originalUuid = uuid;

    // Handle optional braces
    if (uuid.startsWith("{")) {
      if (!uuid.endsWith("}")) {
        throw new IllegalArgumentException("Invalid UUID string: " + originalUuid);
      }
      uuid = uuid.substring(1, uuid.length() - 1);
    }

    // Minimum 36 characters required (standard UUID format: 8-4-4-4-12)
    if (uuid.length() < K_UUID_LENGTH) {
      throw new IllegalArgumentException("Invalid UUID string: " + originalUuid);
    }

    // Check for leading hyphen
    if (uuid.startsWith("-")) {
      throw new IllegalArgumentException("Invalid UUID string: " + originalUuid);
    }

    // Parse 32 hex digits (ignoring hyphens in between)
    long high = 0;
    long low = 0;
    int hexCount = 0;

    for (int i = 0; i < uuid.length(); i++) {
      char c = uuid.charAt(i);
      if (c == '-') {
        continue; // Skip hyphens
      }
      int digit = hexDigit(c);
      if (digit < 0) {
        throw new IllegalArgumentException("Invalid UUID string: " + originalUuid);
      }
      if (hexCount < 16) {
        high = (high << 4) | digit;
      } else {
        low = (low << 4) | digit;
      }
      hexCount++;
    }

    if (hexCount != 32) {
      throw new IllegalArgumentException("Invalid UUID string: " + originalUuid);
    }

    // After parsing, verify there are no trailing characters
    // (uuid must be exactly consumed)
    if (uuid.length() > K_UUID_LENGTH) {
      throw new IllegalArgumentException("Invalid UUID string: " + originalUuid);
    }

    return new long[] {high, low};
  }

  private static int hexDigit(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
    if (c >= 'A' && c <= 'F') return 10 + (c - 'A');
    return -1;
  }

  private TargetRange encodeKeyInternal(
      BiFunction<Integer, String, Value> valueFinder, KeyType keyType) {
    UnsynchronizedByteArrayOutputStream ssKey = new UnsynchronizedByteArrayOutputStream();
    int valueIdx = 0;
    EncodeState state = EncodeState.OK;
    int p = 0;
    for (; p < parts.size(); ++p) {
      final Part part = parts.get(p);
      if (part.kind == Kind.TAG) {
        SsFormat.appendCompositeTag(ssKey, part.tag);
      } else if (part.kind == Kind.VALUE) {
        if (part.random) {
          encodeRandomValuePart(part, ssKey);
          continue;
        }

        int currentIndex = valueIdx;
        if (part.shouldConsumeValueIndex()) {
          valueIdx++;
        }
        ResolvedValue resolved = part.resolveValue(valueFinder, currentIndex);
        if (resolved.failed) {
          state = EncodeState.FAILED;
          break;
        }
        if (!resolved.found) {
          state = part.shouldConsumeValueIndex() ? EncodeState.END_OF_KEYS : EncodeState.FAILED;
          break;
        }
        try {
          encodeSingleValuePart(part, resolved.value, ssKey);
        } catch (IllegalArgumentException e) {
          state = EncodeState.FAILED;
          break;
        }
      } else {
        state = EncodeState.FAILED;
        break;
      }
    }

    ByteString start = ByteString.copyFrom(ssKey.toByteArray());
    ByteString limit = ByteString.EMPTY;
    boolean approximate = false;

    if (p == parts.size() || (keyType != KeyType.FULL_KEY && state == EncodeState.END_OF_KEYS)) {
      if (keyType == KeyType.PREFIX_SUCCESSOR) {
        start = SsFormat.makePrefixSuccessor(start);
      } else if (keyType == KeyType.INDEX_KEY) {
        limit = SsFormat.makePrefixSuccessor(start);
      }
    } else {
      approximate = true;
      limit = SsFormat.makePrefixSuccessor(start);
    }
    return new TargetRange(start, limit, approximate);
  }

  public TargetRange keyToTargetRange(ListValue in) {
    return encodeKeyInternal(
        (index, identifier) -> {
          if (index < 0 || index >= in.getValuesCount()) {
            return null;
          }
          return in.getValues(index);
        },
        isIndex ? KeyType.INDEX_KEY : KeyType.FULL_KEY);
  }

  public TargetRange keyRangeToTargetRange(KeyRange in) {
    TargetRange start;
    switch (in.getStartKeyTypeCase()) {
      case START_CLOSED:
        start =
            encodeKeyInternal(
                (index, id) -> {
                  if (index < 0 || index >= in.getStartClosed().getValuesCount()) return null;
                  return in.getStartClosed().getValues(index);
                },
                KeyType.PREFIX);
        break;
      case START_OPEN:
        start =
            encodeKeyInternal(
                (index, id) -> {
                  if (index < 0 || index >= in.getStartOpen().getValuesCount()) return null;
                  return in.getStartOpen().getValues(index);
                },
                KeyType.PREFIX_SUCCESSOR);
        break;
      default:
        start =
            encodeKeyInternal(
                (index, id) -> {
                  if (index < 0 || index >= ListValue.getDefaultInstance().getValuesCount()) {
                    return null;
                  }
                  return ListValue.getDefaultInstance().getValues(index);
                },
                KeyType.PREFIX);
        start.approximate = true;
        break;
    }

    TargetRange limit;
    switch (in.getEndKeyTypeCase()) {
      case END_CLOSED:
        limit =
            encodeKeyInternal(
                (index, id) -> {
                  if (index < 0 || index >= in.getEndClosed().getValuesCount()) return null;
                  return in.getEndClosed().getValues(index);
                },
                KeyType.PREFIX_SUCCESSOR);
        break;
      case END_OPEN:
        limit =
            encodeKeyInternal(
                (index, id) -> {
                  if (index < 0 || index >= in.getEndOpen().getValuesCount()) return null;
                  return in.getEndOpen().getValues(index);
                },
                KeyType.PREFIX);
        break;
      default:
        limit =
            encodeKeyInternal(
                (index, id) -> {
                  if (index < 0 || index >= ListValue.getDefaultInstance().getValuesCount()) {
                    return null;
                  }
                  return ListValue.getDefaultInstance().getValues(index);
                },
                KeyType.PREFIX_SUCCESSOR);
        limit.approximate = true;
        break;
    }
    ByteString limitKey = limit.approximate ? limit.limit : limit.start;
    return new TargetRange(start.start, limitKey, start.approximate || limit.approximate);
  }

  public TargetRange keySetToTargetRange(KeySet in) {
    if (in.getAll()) {
      return keyRangeToTargetRange(
          KeyRange.newBuilder()
              .setStartClosed(ListValue.getDefaultInstance())
              .setEndClosed(ListValue.getDefaultInstance())
              .build());
    }
    if (in.getRangesCount() == 0) {
      if (in.getKeysCount() == 0) {
        return new TargetRange(ByteString.EMPTY, K_INFINITY, true);
      } else if (in.getKeysCount() == 1) {
        return keyToTargetRange(in.getKeys(0));
      }
    }

    TargetRange target = new TargetRange(K_INFINITY, ByteString.EMPTY, false);
    for (ListValue key : in.getKeysList()) {
      target.mergeFrom(keyToTargetRange(key));
    }
    for (KeyRange range : in.getRangesList()) {
      target.mergeFrom(keyRangeToTargetRange(range));
    }
    return target;
  }

  public TargetRange queryParamsToTargetRange(Struct in) {
    return encodeKeyInternal(
        (index, identifier) -> {
          if (!in.getFieldsMap().containsKey(identifier)) {
            return null;
          }
          return in.getFieldsMap().get(identifier);
        },
        KeyType.FULL_KEY);
  }

  public TargetRange mutationToTargetRange(Mutation in) {
    TargetRange target = new TargetRange(K_INFINITY, ByteString.EMPTY, false);

    switch (in.getOperationCase()) {
      case INSERT:
      case UPDATE:
      case INSERT_OR_UPDATE:
      case REPLACE:
        final Mutation.Write write = getWrite(in);
        for (ListValue values : write.getValuesList()) {
          target.mergeFrom(
              encodeKeyInternal(
                  (index, id) -> {
                    int colIndex = write.getColumnsList().indexOf(id);
                    if (colIndex == -1 || colIndex >= values.getValuesCount()) {
                      return null;
                    }
                    return values.getValues(colIndex);
                  },
                  KeyType.FULL_KEY));
        }
        break;
      case DELETE:
        target.mergeFrom(keySetToTargetRange(in.getDelete().getKeySet()));
        break;
      case SEND:
        target.mergeFrom(keyToTargetRange(in.getSend().getKey()));
        break;
      case ACK:
        target.mergeFrom(keyToTargetRange(in.getAck().getKey()));
        break;
      default:
        break;
    }

    if (target.start.equals(K_INFINITY)) {
      target = new TargetRange(ByteString.EMPTY, K_INFINITY, true);
    }
    return target;
  }

  private Mutation.Write getWrite(Mutation in) {
    switch (in.getOperationCase()) {
      case INSERT:
        return in.getInsert();
      case UPDATE:
        return in.getUpdate();
      case INSERT_OR_UPDATE:
        return in.getInsertOrUpdate();
      case REPLACE:
        return in.getReplace();
      default:
        throw new IllegalArgumentException("Mutation is not a write operation");
    }
  }
}
