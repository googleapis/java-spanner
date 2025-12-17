package com.google.cloud.spanner.spi.v1;

import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class SsFormat {

  /**
   * Makes the given key a prefix successor. This means that the returned key is the smallest
   * possible key that is larger than the input key, and that does not have the input key as a
   * prefix.
   *
   * <p>This is done by flipping the least significant bit of the last byte of the key.
   *
   * @param key The key to make a prefix successor.
   * @return The prefix successor key.
   */
  public static ByteString makePrefixSuccessor(ByteString key) {
    if (key == null || key.isEmpty()) {
      return ByteString.EMPTY;
    }
    byte[] bytes = key.toByteArray();
    if (bytes.length > 0) {
      bytes[bytes.length - 1] = (byte) (bytes[bytes.length - 1] | 1);
    }
    return ByteString.copyFrom(bytes);
  }

  private SsFormat() {}

  // Constants from ssformat.cc
  private static final int IS_KEY = 0x80;
  private static final int TYPE_MASK = 0x7f;

  // HeaderType enum values (selected)
  private static final int TYPE_UINT_1 = 0;
  private static final int TYPE_UINT_9 = 8;
  private static final int TYPE_NEG_INT_8 = 9;
  private static final int TYPE_NEG_INT_1 = 16;
  private static final int TYPE_POS_INT_1 = 17;
  private static final int TYPE_POS_INT_8 = 24;
  private static final int TYPE_STRING = 25;
  private static final int TYPE_NULL_ORDERED_FIRST = 27;
  private static final int TYPE_NULLABLE_NOT_NULL_NULL_ORDERED_FIRST = 28;
  private static final int TYPE_DECREASING_UINT_9 = 32;
  private static final int TYPE_DECREASING_UINT_1 = 40;
  private static final int TYPE_DECREASING_NEG_INT_8 = 41;
  private static final int TYPE_DECREASING_NEG_INT_1 = 48;
  private static final int TYPE_DECREASING_POS_INT_1 = 49;
  private static final int TYPE_DECREASING_POS_INT_8 = 56;
  private static final int TYPE_DECREASING_STRING = 57;
  private static final int TYPE_NULLABLE_NOT_NULL_NULL_ORDERED_LAST = 59;
  private static final int TYPE_NULL_ORDERED_LAST = 60;
  private static final int TYPE_NEG_DOUBLE_8 = 66;
  private static final int TYPE_NEG_DOUBLE_1 = 73;
  private static final int TYPE_POS_DOUBLE_1 = 74;
  private static final int TYPE_POS_DOUBLE_8 = 81;
  private static final int TYPE_DECREASING_NEG_DOUBLE_8 = 82;
  private static final int TYPE_DECREASING_NEG_DOUBLE_1 = 89;
  private static final int TYPE_DECREASING_POS_DOUBLE_1 = 90;
  private static final int TYPE_DECREASING_POS_DOUBLE_8 = 97;

  // EscapeChar enum values
  private static final byte ASCENDING_ZERO_ESCAPE = (byte) 0xf0;
  private static final byte ASCENDING_FF_ESCAPE = (byte) 0x10;
  private static final byte SEP = (byte) 0x78; // 'x'

  // For AppendCompositeTag
  private static final int K_OBJECT_EXISTENCE_TAG = 0x7e;
  private static final int K_MAX_FIELD_TAG = 0xffff;

  public static void appendCompositeTag(ByteArrayOutputStream out, int tag) {
    if (tag == K_OBJECT_EXISTENCE_TAG || tag <= 0 || tag > K_MAX_FIELD_TAG) {
      throw new IllegalArgumentException("Invalid tag value: " + tag);
    }

    if (tag < 16) {
      // Short tag: 000 TTTT S (S is LSB of tag, but here tag is original, so S=0)
      // Encodes as (tag << 1)
      out.write((byte) (tag << 1));
    } else {
      // Long tag
      int shiftedTag = tag << 1; // LSB is 0 for prefix successor
      if (shiftedTag < (1 << (5 + 8))) { // Original tag < 4096
        // Header: num_extra_bytes=1 (01xxxxx), P=payload bits from tag
        // (1 << 5) is 00100000
        // (shiftedTag >> 8) are the 5 MSBs of the payload part of the tag
        out.write((byte) ((1 << 5) | (shiftedTag >> 8)));
        out.write((byte) (shiftedTag & 0xFF));
      } else { // Original tag >= 4096 and <= K_MAX_FIELD_TAG (65535)
        // Header: num_extra_bytes=2 (10xxxxx)
        // (2 << 5) is 01000000
        out.write((byte) ((2 << 5) | (shiftedTag >> 16)));
        out.write((byte) ((shiftedTag >> 8) & 0xFF));
        out.write((byte) (shiftedTag & 0xFF));
      }
    }
  }

  public static void appendNullOrderedFirst(ByteArrayOutputStream out) {
    out.write((byte) (IS_KEY | TYPE_NULL_ORDERED_FIRST));
    out.write((byte) 0);
  }

  public static void appendNullOrderedLast(ByteArrayOutputStream out) {
    out.write((byte) (IS_KEY | TYPE_NULL_ORDERED_LAST));
    out.write((byte) 0);
  }

  public static void appendNotNullMarkerNullOrderedFirst(ByteArrayOutputStream out) {
    out.write((byte) (IS_KEY | TYPE_NULLABLE_NOT_NULL_NULL_ORDERED_FIRST));
  }

  public static void appendNotNullMarkerNullOrderedLast(ByteArrayOutputStream out) {
    out.write((byte) (IS_KEY | TYPE_NULLABLE_NOT_NULL_NULL_ORDERED_LAST));
  }

  public static void appendUnsignedIntIncreasing(ByteArrayOutputStream out, long val) {
    if (val < 0) {
      throw new IllegalArgumentException("Unsigned int cannot be negative: " + val);
    }
    byte[] buf = new byte[9]; // Max 9 bytes for value payload
    int len = 0;

    long tempVal = val;
    buf[8 - len] = (byte) ((tempVal & 0x7F) << 1); // LSB is prefix-successor bit (0)
    tempVal >>= 7;
    len++;

    while (tempVal > 0) {
      buf[8 - len] = (byte) (tempVal & 0xFF);
      tempVal >>= 8;
      len++;
    }

    out.write((byte) (IS_KEY | (TYPE_UINT_1 + len - 1)));
    for (int i = 0; i < len; i++) {
      out.write((byte) (buf[8 - len + 1 + i] & 0xFF));
    }
  }

  public static void appendUnsignedIntDecreasing(ByteArrayOutputStream out, long val) {
    if (val < 0) {
      throw new IllegalArgumentException("Unsigned int cannot be negative: " + val);
    }
    byte[] buf = new byte[9];
    int len = 0;
    long tempVal = val;

    // InvertByte(val & 0x7f) << 1
    buf[8 - len] = (byte) ((~(tempVal & 0x7F) & 0x7F) << 1);
    tempVal >>= 7;
    len++;

    while (tempVal > 0) {
      buf[8 - len] = (byte) (~(tempVal & 0xFF));
      tempVal >>= 8;
      len++;
    }
    // If val was 0, loop doesn't run for len > 1. If len is still 1, all bits of tempVal (0) are
    // covered.
    // If val was large, but remaining tempVal became 0, this is correct.
    // If tempVal was 0 initially, buf[8] has (~0 & 0x7f) << 1. len = 1.
    // If tempVal was >0 but became 0 after some shifts, buf[8-len] has inverted last byte.

    out.write((byte) (IS_KEY | (TYPE_DECREASING_UINT_1 - len + 1)));
    for (int i = 0; i < len; i++) {
      out.write((byte) (buf[8 - len + 1 + i] & 0xFF));
    }
  }

  private static void appendIntInternal(
      ByteArrayOutputStream out, long val, boolean decreasing, boolean isDouble) {
    if (decreasing) {
      val = ~val;
    }

    byte[] buf = new byte[8]; // Max 8 bytes for payload
    int len = 0;
    long tempVal = val;

    if (tempVal >= 0) {
      buf[7 - len] = (byte) ((tempVal & 0x7F) << 1);
      tempVal >>= 7;
      len++;
      while (tempVal > 0) {
        buf[7 - len] = (byte) (tempVal & 0xFF);
        tempVal >>= 8;
        len++;
      }
    } else { // tempVal < 0
      // For negative numbers, extend sign bit after shifting
      buf[7 - len] = (byte) ((tempVal & 0x7F) << 1);
      // Simulate sign extension for right shift of negative number
      // (x >> 7) | 0xFE00000000000000ULL; (if x has 64 bits)
      // In Java, right shift `>>` on negative longs performs sign extension.
      tempVal >>= 7;
      len++;
      while (tempVal != -1L) { // Loop until all remaining bits are 1s (sign extension)
        buf[7 - len] = (byte) (tempVal & 0xFF);
        tempVal >>= 8;
        len++;
        if (len > 8) throw new AssertionError("Signed int encoding overflow");
      }
    }

    int type;
    if (val >= 0) { // Original val before potential bit-negation for decreasing
      if (!decreasing) {
        type = isDouble ? (TYPE_POS_DOUBLE_1 + len - 1) : (TYPE_POS_INT_1 + len - 1);
      } else {
        type =
            isDouble
                ? (TYPE_DECREASING_POS_DOUBLE_1 + len - 1)
                : (TYPE_DECREASING_POS_INT_1 + len - 1);
      }
    } else {
      if (!decreasing) {
        type = isDouble ? (TYPE_NEG_DOUBLE_1 - len + 1) : (TYPE_NEG_INT_1 - len + 1);
      } else {
        type =
            isDouble
                ? (TYPE_DECREASING_NEG_DOUBLE_1 - len + 1)
                : (TYPE_DECREASING_NEG_INT_1 - len + 1);
      }
    }
    out.write((byte) (IS_KEY | type));
    for (int i = 0; i < len; i++) {
      out.write((byte) (buf[7 - len + 1 + i] & 0xFF));
    }
  }

  public static void appendIntIncreasing(ByteArrayOutputStream out, long value) {
    appendIntInternal(out, value, false, false);
  }

  public static void appendIntDecreasing(ByteArrayOutputStream out, long value) {
    appendIntInternal(out, value, true, false);
  }

  public static void appendDoubleIncreasing(ByteArrayOutputStream out, double value) {
    long enc = Double.doubleToRawLongBits(value);
    if (enc < 0) {
      enc =
          Long.MIN_VALUE
              - enc; // kint64min - enc (equivalent to ~enc for negative values due to 2's
      // complement)
    }
    appendIntInternal(out, enc, false, true);
  }

  public static void appendDoubleDecreasing(ByteArrayOutputStream out, double value) {
    long enc = Double.doubleToRawLongBits(value);
    if (enc < 0) {
      enc = Long.MIN_VALUE - enc;
    }
    appendIntInternal(out, enc, true, true);
  }

  private static void appendByteSequence(
      ByteArrayOutputStream out, byte[] bytes, boolean decreasing) {
    out.write((byte) (IS_KEY | (decreasing ? TYPE_DECREASING_STRING : TYPE_STRING)));

    for (byte b : bytes) {
      byte currentByte = decreasing ? (byte) ~b : b;
      int unsignedByte = currentByte & 0xFF;
      if (unsignedByte == 0x00) {
        out.write((byte) 0x00);
        out.write(
            decreasing
                ? ASCENDING_ZERO_ESCAPE
                : ASCENDING_ZERO_ESCAPE); // After inversion, 0xFF becomes 0x00. Escape for 0x00
        // (inverted) is F0.
        // If increasing, 0x00 -> 0x00 F0.
      } else if (unsignedByte == 0xFF) {
        out.write((byte) 0xFF);
        out.write(
            decreasing
                ? ASCENDING_FF_ESCAPE
                : ASCENDING_FF_ESCAPE); // After inversion, 0x00 becomes 0xFF. Escape for 0xFF
        // (inverted) is 0x10.
        // If increasing, 0xFF -> 0xFF 0x10.
      } else {
        out.write((byte) unsignedByte);
      }
    }
    // Terminator
    out.write((byte) (decreasing ? 0xFF : 0x00));
    out.write(SEP);
  }

  public static void appendStringIncreasing(ByteArrayOutputStream out, String value) {
    appendByteSequence(out, value.getBytes(StandardCharsets.UTF_8), false);
  }

  public static void appendStringDecreasing(ByteArrayOutputStream out, String value) {
    appendByteSequence(out, value.getBytes(StandardCharsets.UTF_8), true);
  }

  public static void appendBytesIncreasing(ByteArrayOutputStream out, byte[] value) {
    appendByteSequence(out, value, false);
  }

  public static void appendBytesDecreasing(ByteArrayOutputStream out, byte[] value) {
    appendByteSequence(out, value, true);
  }
}
