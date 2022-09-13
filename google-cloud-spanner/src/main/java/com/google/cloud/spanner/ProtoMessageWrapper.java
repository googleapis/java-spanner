/*
 * Copyright 2022 Google LLC
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

import com.google.protobuf.AbstractMessage;
import java.util.Arrays;

public class ProtoMessageWrapper {
  AbstractMessage message;
  byte[] serializedMessage;

  ProtoMessageWrapper(AbstractMessage m) {
    this.message = m;
    this.serializedMessage = m.toByteArray();
  }

  ProtoMessageWrapper(byte[] serializedMessage) {
    this.serializedMessage = serializedMessage;
  }

  @Override
  public String toString() {
    if (message != null) {
      return message.toString();
    }
    return Arrays.toString(serializedMessage);
  }
}
