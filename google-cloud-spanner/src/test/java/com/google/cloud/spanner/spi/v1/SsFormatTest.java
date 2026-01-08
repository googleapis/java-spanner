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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link SsFormat}. */
@RunWith(JUnit4.class)
public class SsFormatTest {

  @Test
  public void testMakePrefixSuccessor() {
    // Empty input returns empty
    assertEquals(ByteString.EMPTY, SsFormat.makePrefixSuccessor(ByteString.EMPTY));
    assertEquals(ByteString.EMPTY, SsFormat.makePrefixSuccessor(null));

    // Single byte - LSB should be set
    ByteString input = ByteString.copyFrom(new byte[] {0x00});
    ByteString result = SsFormat.makePrefixSuccessor(input);
    assertEquals(1, result.size());
    assertEquals(0x01, result.byteAt(0) & 0xFF);

    // Multiple bytes - only last byte's LSB should be set
    input = ByteString.copyFrom(new byte[] {0x12, 0x34, 0x00});
    result = SsFormat.makePrefixSuccessor(input);
    assertEquals(3, result.size());
    assertEquals(0x12, result.byteAt(0) & 0xFF);
    assertEquals(0x34, result.byteAt(1) & 0xFF);
    assertEquals(0x01, result.byteAt(2) & 0xFF);
  }

  @Test
  public void testAppendCompositeTag() {
    // Short tag (< 16)
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendCompositeTag(out, 5);
    byte[] result = out.toByteArray();
    assertEquals(1, result.length);
    assertEquals(10, result[0] & 0xFF); // 5 << 1 = 10

    // Medium tag (16 <= tag < 4096)
    out = new ByteArrayOutputStream();
    SsFormat.appendCompositeTag(out, 100);
    result = out.toByteArray();
    assertEquals(2, result.length);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAppendCompositeTagInvalidTag() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendCompositeTag(out, 0); // Invalid tag
  }

  @Test
  public void testAppendUnsignedIntIncreasing() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendUnsignedIntIncreasing(out, 0);
    byte[] result = out.toByteArray();
    assertTrue(result.length >= 2); // Header + at least 1 byte

    // First byte should have IS_KEY bit set (0x80)
    assertTrue((result[0] & 0x80) != 0);
  }

  @Test
  public void testAppendUnsignedIntDecreasing() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendUnsignedIntDecreasing(out, 0);
    byte[] result = out.toByteArray();
    assertTrue(result.length >= 2);
    assertTrue((result[0] & 0x80) != 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAppendUnsignedIntNegative() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendUnsignedIntIncreasing(out, -1);
  }

  @Test
  public void testAppendIntIncreasing() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendIntIncreasing(out, 0);
    byte[] result = out.toByteArray();
    assertTrue(result.length >= 2);

    // Test negative number
    out = new ByteArrayOutputStream();
    SsFormat.appendIntIncreasing(out, -1);
    result = out.toByteArray();
    assertTrue(result.length >= 2);
  }

  @Test
  public void testAppendIntDecreasing() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendIntDecreasing(out, 0);
    byte[] result = out.toByteArray();
    assertTrue(result.length >= 2);

    out = new ByteArrayOutputStream();
    SsFormat.appendIntDecreasing(out, -1);
    result = out.toByteArray();
    assertTrue(result.length >= 2);
  }

  @Test
  public void testAppendStringIncreasing() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendStringIncreasing(out, "hello");
    byte[] result = out.toByteArray();
    assertTrue(result.length > 5); // Header + string + terminator

    // First byte should have IS_KEY bit set and TYPE_STRING
    assertTrue((result[0] & 0x80) != 0);
  }

  @Test
  public void testAppendStringDecreasing() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendStringDecreasing(out, "hello");
    byte[] result = out.toByteArray();
    assertTrue(result.length > 5);
    assertTrue((result[0] & 0x80) != 0);
  }

  @Test
  public void testAppendBytesIncreasing() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendBytesIncreasing(out, new byte[] {0x01, 0x02, 0x03});
    byte[] result = out.toByteArray();
    assertTrue(result.length > 3);
  }

  @Test
  public void testAppendDoubleIncreasing() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendDoubleIncreasing(out, 1.5);
    byte[] result = out.toByteArray();
    assertTrue(result.length >= 2);

    // Test negative double
    out = new ByteArrayOutputStream();
    SsFormat.appendDoubleIncreasing(out, -1.5);
    result = out.toByteArray();
    assertTrue(result.length >= 2);
  }

  @Test
  public void testAppendDoubleDecreasing() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendDoubleDecreasing(out, 1.5);
    byte[] result = out.toByteArray();
    assertTrue(result.length >= 2);
  }

  @Test
  public void testAppendNullMarkers() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendNullOrderedFirst(out);
    byte[] result = out.toByteArray();
    assertEquals(2, result.length);
    assertTrue((result[0] & 0x80) != 0);

    out = new ByteArrayOutputStream();
    SsFormat.appendNullOrderedLast(out);
    result = out.toByteArray();
    assertEquals(2, result.length);
  }

  @Test
  public void testAppendNotNullMarkers() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendNotNullMarkerNullOrderedFirst(out);
    byte[] result = out.toByteArray();
    assertEquals(1, result.length);

    out = new ByteArrayOutputStream();
    SsFormat.appendNotNullMarkerNullOrderedLast(out);
    result = out.toByteArray();
    assertEquals(1, result.length);
  }

  @Test
  public void testEncodeTimestamp() {
    byte[] result = SsFormat.encodeTimestamp(0, 0);
    assertEquals(12, result.length);

    result = SsFormat.encodeTimestamp(1234567890L, 123456789);
    assertEquals(12, result.length);
  }

  @Test
  public void testEncodeUuid() {
    byte[] result = SsFormat.encodeUuid(0x1234567890ABCDEFL, 0xFEDCBA0987654321L);
    assertEquals(16, result.length);

    // Verify big-endian encoding
    assertEquals(0x12, result[0] & 0xFF);
    assertEquals(0x34, result[1] & 0xFF);
    assertEquals(0xFE, result[8] & 0xFF);
    assertEquals(0xDC, result[9] & 0xFF);
  }

  @Test
  public void testStringEscaping() {
    // Test that 0x00 and 0xFF bytes are properly escaped
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    SsFormat.appendBytesIncreasing(out, new byte[] {0x00, (byte) 0xFF, 0x42});
    byte[] result = out.toByteArray();
    // Result should be longer due to escaping
    assertTrue(result.length > 5); // header + 3 original bytes + escapes + terminator
  }

  @Test
  public void testOrderPreservation() {
    // Verify that smaller integers encode to smaller byte sequences (lexicographically)
    ByteArrayOutputStream out1 = new ByteArrayOutputStream();
    SsFormat.appendIntIncreasing(out1, 100);

    ByteArrayOutputStream out2 = new ByteArrayOutputStream();
    SsFormat.appendIntIncreasing(out2, 200);

    ByteString bs1 = ByteString.copyFrom(out1.toByteArray());
    ByteString bs2 = ByteString.copyFrom(out2.toByteArray());

    assertTrue(ByteString.unsignedLexicographicalComparator().compare(bs1, bs2) < 0);
  }
}
