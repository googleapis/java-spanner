/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.executor.spanner;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/** Helper class with JNI methods to handle transcoding for NUMERIC Values. */
public final class NumericTranscoder {
  public static final String MIN_NUMERIC = "-99999999999999999999999999999.999999999";
  public static final String MAX_NUMERIC = "99999999999999999999999999999.999999999";
  public static final int MIN_ENCODED_NUMERIC_SIZE = 1;
  public static final int MIN_ENCODED_NON_ZERO_NUMERIC_SIZE = 6;
  public static final int MAX_ENCODED_NUMERIC_SIZE = 22;
  public static final int MAX_DECODED_NUMERIC_SIZE = 50;

  public static ByteString encode(String decoded) {
    Preconditions.checkNotNull(decoded, "NULL argument provided to encode function");
    if (decoded.length() > MAX_DECODED_NUMERIC_SIZE) {
      throw new IllegalArgumentException(
          String.format("Invalid NUMERIC value: %s is too long", decoded));
    }
    byte[] encoded = encodeNumeric(decoded, decoded.length());
    if (encoded == null) {
      throw new IllegalArgumentException(
          String.format("Failed to encode numeric value: %s", decoded));
    }
    return ByteString.copyFrom(encoded);
  }

  public static String decode(byte[] encoded) {
    Preconditions.checkNotNull(encoded, "NULL argument provided to decode function");
    return decode(encoded, 0, encoded.length);
  }

  public static String decode(byte[] encoded, int offset, int length) {
    Preconditions.checkNotNull(encoded, "NULL argument provided to decode function");
    Preconditions.checkPositionIndexes(offset, offset + length, encoded.length);
    if (length < MIN_ENCODED_NUMERIC_SIZE || isInvalidLength(encoded[offset], length)) {
      throw new IllegalArgumentException(
          String.format("Invalid encoded numeric value: %s", toDisplayString(encoded)));
    }
    return decodeNumeric(encoded, offset, length);
  }

  public static String decode(ByteBuffer encoded) {
    Preconditions.checkNotNull(encoded, "NULL argument provided to decode function");
    if (encoded.remaining() < MIN_ENCODED_NUMERIC_SIZE
        || isInvalidLength(encoded.get(encoded.position()), encoded.remaining())) {
      throw new IllegalArgumentException(
          String.format("Invalid encoded numeric value: %s", toDisplayString(encoded)));
    }
    if (encoded.isDirect()) {
      return decodeNumeric(encoded);
    }
    // Non-direct byte buffer, JNI will not be able to get the address for the buffer.
    if (encoded.hasArray()) {
      // We have access to the underlying array.
      return decodeNumeric(
          encoded.array(), encoded.arrayOffset() + encoded.position(), encoded.remaining());
    }
    // Worst case scenario. The buffer is not direct and we cannot access the underlying array.
    // Slice the buffer to avoid mutating it and copy the data to a new byte array.
    encoded = encoded.slice();
    int length = encoded.remaining();
    byte[] data = new byte[length];
    encoded.get(data);
    return decodeNumeric(data, 0, length);
  }

  public static String decode(ByteString encoded) {
    return decode(encoded.asReadOnlyByteBuffer());
  }

  private static boolean isInvalidLength(byte first, int length) {
    Preconditions.checkArgument(length > 0);
    return (length == 1 && first != Byte.MIN_VALUE)
        || (length != 1 && first != Byte.MIN_VALUE && length < MIN_ENCODED_NON_ZERO_NUMERIC_SIZE)
        || length > MAX_ENCODED_NUMERIC_SIZE;
  }

  private static String toDisplayString(byte[] encoded, int offset, int length) {
    return BaseEncoding.base16().upperCase().encode(encoded, offset, length);
  }

  private static String toDisplayString(byte[] encoded) {
    return toDisplayString(encoded, 0, encoded.length);
  }

  private static String toDisplayString(ByteBuffer encoded) {
    if (encoded.hasArray()) {
      return toDisplayString(
          encoded.array(), encoded.arrayOffset() + encoded.position(), encoded.remaining());
    }
    encoded = encoded.slice();
    int length = encoded.remaining();
    byte[] data = new byte[length];
    encoded.get(data);
    return toDisplayString(data, 0, length);
  }

  private static String decodeNumeric(byte[] encoded, int offset, int length) {
    String decoded = decodeNumericByteArray(encoded, offset, length);
    if (decoded == null) {
      throw new IllegalArgumentException(
          String.format("Failed to decode numeric value: %s", toDisplayString(encoded)));
    }
    return decoded;
  }

  private static String decodeNumeric(ByteBuffer encoded) {
    String decoded = decodeNumericByteBuffer(encoded, encoded.position(), encoded.remaining());
    if (decoded == null) {
      throw new IllegalArgumentException(
          String.format("Failed to decode numeric value: %s", toDisplayString(encoded)));
    }
    return decoded;
  }

  // Wrapper around native methods for encoding and decoding numeric values.

  // Returns the encoded numeric value, or null if an error occurred during encoding.
  private static native byte[] encodeNumeric(String decoded, int length);

  // Returns the decode ascii numeric value, or null if an error occurred during decoding.
  private static native String decodeNumericByteArray(byte[] encoded, int offset, int length);

  // Returns the decode ascii numeric value, or null if an error occurred during decoding.
  private static native String decodeNumericByteBuffer(Buffer encoded, int position, int limit);

  private NumericTranscoder() {}
}
