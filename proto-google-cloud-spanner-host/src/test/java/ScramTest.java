/*
 * Copyright 2017 Google LLC
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.google.cloud.Scram;
import com.google.protobuf.ByteString;
import org.junit.Test;

public class ScramTest {

  @Test
  public void testNonceGeneration() {
    byte[] nonce1 = Scram.nonce();
    byte[] nonce2 = Scram.nonce();
    assertNotNull(nonce1);
    assertNotNull(nonce2);
    assertEquals(16, nonce1.length);
    assertEquals(16, nonce2.length);
    assertNotEquals(ByteString.copyFrom(nonce1), ByteString.copyFrom(nonce2));
  }

  @Test
  public void testSaltPassword() throws Exception {
    byte[] salt = "randomSalt".getBytes();
    int iterations = 3;
    byte[] hashedPassword = Scram.saltPassword("testPass", salt, iterations);
    assertNotNull(hashedPassword);
    assertEquals(32, hashedPassword.length);

    byte[] emptySalt = new byte[0];
    Exception saltException =
        assertThrows(
            Exception.class,
            () -> {
              Scram.saltPassword("testPass", emptySalt, iterations);
            });
    assertEquals("No salt found", saltException.getMessage());

    Exception iterationException =
        assertThrows(
            Exception.class,
            () -> {
              Scram.saltPassword("testPass", salt, 0);
            });
    assertEquals("No iteration count found", iterationException.getMessage());
  }
}
