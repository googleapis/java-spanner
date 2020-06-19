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

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Struct;
import com.google.spanner.v1.ResultSetStats;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class ForceRetryStrategy implements RetryAbortedStrategy {
  private static final Logger logger = Logger.getLogger(ForceRetryStrategy.class.getName());

  private final ReadWriteTransaction transaction;

  public ForceRetryStrategy(ReadWriteTransaction transaction) {
    this.transaction = transaction;
  }

  @Override
  public ReadWriteTransaction.RetryStatementResultSet createRetriableStarement(
      final ResultSet rs,
      final StatementParser.ParsedStatement statement,
      final AnalyzeMode analyzeMode,
      final Options.QueryOption... options) {
    return new ReadWriteTransaction.RetryStatementResultSet() {
      private ResultSet delegate = rs;

      public boolean next() throws SpannerException {
        return delegate.next();
      }

      public Struct getCurrentRowAsStruct() {
        return delegate.getCurrentRowAsStruct();
      }

      public void close() {
        delegate.close();
      }

      @Nullable
      public ResultSetStats getStats() {
        return delegate.getStats();
      }

      public com.google.cloud.spanner.Type getType() {
        return delegate.getType();
      }

      public int getColumnCount() {
        return delegate.getColumnCount();
      }

      public int getColumnIndex(String columnName) {
        return delegate.getColumnIndex(columnName);
      }

      public com.google.cloud.spanner.Type getColumnType(int columnIndex) {
        return delegate.getColumnType(columnIndex);
      }

      public com.google.cloud.spanner.Type getColumnType(String columnName) {
        return delegate.getColumnType(columnName);
      }

      public boolean isNull(int columnIndex) {
        return delegate.isNull(columnIndex);
      }

      public boolean isNull(String columnName) {
        return delegate.isNull(columnName);
      }

      public boolean getBoolean(int columnIndex) {
        return delegate.getBoolean(columnIndex);
      }

      public boolean getBoolean(String columnName) {
        return delegate.getBoolean(columnName);
      }

      public long getLong(int columnIndex) {
        return delegate.getLong(columnIndex);
      }

      public long getLong(String columnName) {
        return delegate.getLong(columnName);
      }

      public double getDouble(int columnIndex) {
        return delegate.getDouble(columnIndex);
      }

      public double getDouble(String columnName) {
        return delegate.getDouble(columnName);
      }

      public String getString(int columnIndex) {
        return delegate.getString(columnIndex);
      }

      public String getString(String columnName) {
        return delegate.getString(columnName);
      }

      public ByteArray getBytes(int columnIndex) {
        return delegate.getBytes(columnIndex);
      }

      public ByteArray getBytes(String columnName) {
        return delegate.getBytes(columnName);
      }

      public Timestamp getTimestamp(int columnIndex) {
        return delegate.getTimestamp(columnIndex);
      }

      public Timestamp getTimestamp(String columnName) {
        return delegate.getTimestamp(columnName);
      }

      public Date getDate(int columnIndex) {
        return delegate.getDate(columnIndex);
      }

      public Date getDate(String columnName) {
        return delegate.getDate(columnName);
      }

      public boolean[] getBooleanArray(int columnIndex) {
        return delegate.getBooleanArray(columnIndex);
      }

      public boolean[] getBooleanArray(String columnName) {
        return delegate.getBooleanArray(columnName);
      }

      public List<Boolean> getBooleanList(int columnIndex) {
        return delegate.getBooleanList(columnIndex);
      }

      public List<Boolean> getBooleanList(String columnName) {
        return delegate.getBooleanList(columnName);
      }

      public long[] getLongArray(int columnIndex) {
        return delegate.getLongArray(columnIndex);
      }

      public long[] getLongArray(String columnName) {
        return delegate.getLongArray(columnName);
      }

      public List<Long> getLongList(int columnIndex) {
        return delegate.getLongList(columnIndex);
      }

      public List<Long> getLongList(String columnName) {
        return delegate.getLongList(columnName);
      }

      public double[] getDoubleArray(int columnIndex) {
        return delegate.getDoubleArray(columnIndex);
      }

      public double[] getDoubleArray(String columnName) {
        return delegate.getDoubleArray(columnName);
      }

      public List<Double> getDoubleList(int columnIndex) {
        return delegate.getDoubleList(columnIndex);
      }

      public List<Double> getDoubleList(String columnName) {
        return delegate.getDoubleList(columnName);
      }

      public List<String> getStringList(int columnIndex) {
        return delegate.getStringList(columnIndex);
      }

      public List<String> getStringList(String columnName) {
        return delegate.getStringList(columnName);
      }

      public List<ByteArray> getBytesList(int columnIndex) {
        return delegate.getBytesList(columnIndex);
      }

      public List<ByteArray> getBytesList(String columnName) {
        return delegate.getBytesList(columnName);
      }

      public List<Timestamp> getTimestampList(int columnIndex) {
        return delegate.getTimestampList(columnIndex);
      }

      public List<Timestamp> getTimestampList(String columnName) {
        return delegate.getTimestampList(columnName);
      }

      public List<Date> getDateList(int columnIndex) {
        return delegate.getDateList(columnIndex);
      }

      public List<Date> getDateList(String columnName) {
        return delegate.getDateList(columnName);
      }

      public List<Struct> getStructList(int columnIndex) {
        return delegate.getStructList(columnIndex);
      }

      public List<Struct> getStructList(String columnName) {
        return delegate.getStructList(columnName);
      }

      @Override
      public void retry(AbortedException aborted) throws AbortedException {
        if (logger.isLoggable(Level.FINE)) {
          logger.log(
              Level.FINE,
              "About to retry SQL transaction\n[{0}]\nAfter getting a message: [{1}]",
              new Object[] {statement.getSqlWithoutComments(), aborted.getMessage()});
        }
        transaction
            .getStatementExecutor()
            .invokeInterceptors(statement, StatementExecutionStep.RETRY_STATEMENT, transaction);
        delegate =
            DirectExecuteResultSet.ofResultSet(
                transaction.internalExecuteQuery(statement, analyzeMode, options));
      }
    };
  }
}
