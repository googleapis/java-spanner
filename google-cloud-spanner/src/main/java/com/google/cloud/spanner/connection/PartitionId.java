/*
 * Copyright 2023 Google LLC
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

import com.google.cloud.spanner.BatchTransactionId;
import com.google.cloud.spanner.Partition;
import com.google.cloud.spanner.SpannerExceptionFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Contains a reference to a {@link BatchTransactionId} and a {@link Partition}. The combination of
 * these two are needed to execute a partition of a partitioned query on a {@link Connection}. A
 * {@link PartitionId} can safely be given to a different connection and/or host to be executed
 * there.
 */
public class PartitionId implements Serializable {
  private static final long serialVersionUID = 239487275L;

  private final BatchTransactionId transactionId;
  private final Partition partition;

  /**
   * Deserializes a string representation of a {@link PartitionId}. The string must have been
   * created with the {@link #encodeToString(BatchTransactionId, Partition)} method.
   */
  public static PartitionId decodeFromString(String id) {
    try (ObjectInputStream objectInputStream =
        new ObjectInputStream(
            new GZIPInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(id))))) {
      return (PartitionId) objectInputStream.readObject();
    } catch (Exception exception) {
      throw SpannerExceptionFactory.newSpannerException(exception);
    }
  }

  /**
   * @return A string-encoded version of this {@link PartitionId}. This encoded version can be sent
   *     to any other {@link Connection} to be executed there, including connections on different
   *     hosts than the current host.
   */
  public static String encodeToString(BatchTransactionId transactionId, Partition partition) {
    PartitionId id = new PartitionId(transactionId, partition);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (ObjectOutputStream objectOutputStream =
        new ObjectOutputStream(new GZIPOutputStream(byteArrayOutputStream))) {
      objectOutputStream.writeObject(id);
    } catch (Exception exception) {
      throw SpannerExceptionFactory.newSpannerException(exception);
    }
    return Base64.getUrlEncoder().encodeToString(byteArrayOutputStream.toByteArray());
  }

  private PartitionId(BatchTransactionId transactionId, Partition partition) {
    this.transactionId = transactionId;
    this.partition = partition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PartitionId)) {
      return false;
    }
    PartitionId other = (PartitionId) o;
    return Objects.equals(this.transactionId, other.transactionId)
        && Objects.equals(this.partition, other.partition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.transactionId, this.partition);
  }

  public BatchTransactionId getTransactionId() {
    return transactionId;
  }

  public Partition getPartition() {
    return partition;
  }
}
