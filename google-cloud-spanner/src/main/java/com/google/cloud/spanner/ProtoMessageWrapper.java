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
