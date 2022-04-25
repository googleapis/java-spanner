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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.ReadContext.QueryAnalyzeMode;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.Type;
import com.google.protobuf.Duration;
import com.google.spanner.v1.ResultSetStats;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ConnectionStatementExecutorTest {
  @Parameter public Dialect dialect;

  @Parameters(name = "dialect = {0}")
  public static Object[] data() {
    return Dialect.values();
  }

  private ConnectionImpl connection;
  private ConnectionStatementExecutorImpl subject;

  @Before
  public void createSubject() {
    connection = mock(ConnectionImpl.class);
    when(connection.getAutocommitDmlMode()).thenReturn(AutocommitDmlMode.TRANSACTIONAL);
    when(connection.getReadOnlyStaleness()).thenReturn(TimestampBound.strong());
    when(connection.getDialect()).thenReturn(dialect);
    subject = new ConnectionStatementExecutorImpl(connection);
  }

  @Test
  public void testGetConnection() {
    assertThat(subject.getConnection(), is(equalTo(connection)));
  }

  @Test
  public void testStatementBeginTransaction() {
    subject.statementBeginTransaction();
    verify(connection).beginTransaction();
  }

  @Test
  public void testStatementCommit() {
    subject.statementCommit();
    verify(connection).commit();
  }

  @Test
  public void testStatementGetAutocommit() {
    subject.statementShowAutocommit();
    verify(connection).isAutocommit();
  }

  @Test
  public void testStatementGetAutocommitDmlMode() {
    subject.statementShowAutocommitDmlMode();
    verify(connection).getAutocommitDmlMode();
  }

  @Test
  public void testStatementGetCommitTimestamp() {
    subject.statementShowCommitTimestamp();
    verify(connection).getCommitTimestampOrNull();
  }

  @Test
  public void testStatementGetReadOnly() {
    subject.statementShowReadOnly();
    verify(connection).isReadOnly();
  }

  @Test
  public void testStatementGetReadOnlyStaleness() {
    subject.statementShowReadOnlyStaleness();
    verify(connection).getReadOnlyStaleness();
  }

  @Test
  public void testStatementGetOptimizerVersion() {
    subject.statementShowOptimizerVersion();
    verify(connection).getOptimizerVersion();
  }

  @Test
  public void testStatementGetOptimizerStatisticsPackage() {
    subject.statementShowOptimizerStatisticsPackage();
    verify(connection).getOptimizerStatisticsPackage();
  }

  @Test
  public void testStatementGetReadTimestamp() {
    subject.statementShowReadTimestamp();
    verify(connection).getReadTimestampOrNull();
  }

  @Test
  public void testStatementGetStatementTimeout() {
    subject.statementSetStatementTimeout(Duration.newBuilder().setSeconds(1L).build());
    when(connection.hasStatementTimeout()).thenReturn(true);
    subject.statementShowStatementTimeout();
    verify(connection, atLeastOnce()).getStatementTimeout(any(TimeUnit.class));
    subject.statementSetStatementTimeout(Duration.getDefaultInstance());
    when(connection.hasStatementTimeout()).thenReturn(false);
  }

  @Test
  public void testStatementRollback() {
    subject.statementRollback();
    verify(connection).rollback();
  }

  @Test
  public void testStatementSetAutocommit() {
    subject.statementSetAutocommit(Boolean.TRUE);
    verify(connection).setAutocommit(true);
    subject.statementSetAutocommit(Boolean.FALSE);
    verify(connection).setAutocommit(false);
  }

  @Test
  public void testStatementSetAutocommitDmlMode() {
    subject.statementSetAutocommitDmlMode(AutocommitDmlMode.PARTITIONED_NON_ATOMIC);
    verify(connection).setAutocommitDmlMode(AutocommitDmlMode.PARTITIONED_NON_ATOMIC);
    subject.statementSetAutocommitDmlMode(AutocommitDmlMode.TRANSACTIONAL);
    verify(connection).setAutocommitDmlMode(AutocommitDmlMode.TRANSACTIONAL);
  }

  @Test
  public void testStatementSetReadOnly() {
    subject.statementSetReadOnly(Boolean.TRUE);
    verify(connection).setReadOnly(true);
    subject.statementSetReadOnly(Boolean.FALSE);
    verify(connection).setReadOnly(false);
  }

  @Test
  public void testStatementSetReadOnlyStaleness() {
    subject.statementSetReadOnlyStaleness(TimestampBound.strong());
    verify(connection).setReadOnlyStaleness(TimestampBound.strong());

    subject.statementSetReadOnlyStaleness(
        TimestampBound.ofReadTimestamp(Timestamp.parseTimestamp("2018-10-31T10:11:12.123Z")));
    verify(connection)
        .setReadOnlyStaleness(
            TimestampBound.ofReadTimestamp(Timestamp.parseTimestamp("2018-10-31T10:11:12.123Z")));

    subject.statementSetReadOnlyStaleness(
        TimestampBound.ofMinReadTimestamp(Timestamp.parseTimestamp("2018-10-31T10:11:12.123Z")));
    verify(connection)
        .setReadOnlyStaleness(
            TimestampBound.ofReadTimestamp(Timestamp.parseTimestamp("2018-10-31T10:11:12.123Z")));

    subject.statementSetReadOnlyStaleness(TimestampBound.ofExactStaleness(10L, TimeUnit.SECONDS));
    verify(connection).setReadOnlyStaleness(TimestampBound.ofExactStaleness(10L, TimeUnit.SECONDS));

    subject.statementSetReadOnlyStaleness(
        TimestampBound.ofMaxStaleness(20L, TimeUnit.MILLISECONDS));
    verify(connection)
        .setReadOnlyStaleness(TimestampBound.ofMaxStaleness(20L, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testStatementSetOptimizerVersion() {
    subject.statementSetOptimizerVersion("1");
    verify(connection).setOptimizerVersion("1");
    subject.statementSetOptimizerVersion("");
    verify(connection).setOptimizerVersion("");
    subject.statementSetOptimizerVersion("LATEST");
    verify(connection).setOptimizerVersion("LATEST");
  }

  @Test
  public void testStatementSetOptimizerStatisticsPackage() {
    subject.statementSetOptimizerStatisticsPackage("custom-package");
    verify(connection).setOptimizerStatisticsPackage("custom-package");
    subject.statementSetOptimizerStatisticsPackage("");
    verify(connection).setOptimizerStatisticsPackage("");
  }

  @Test
  public void testStatementSetStatementTimeout() {
    subject.statementSetStatementTimeout(Duration.newBuilder().setNanos(100).build());
    verify(connection).setStatementTimeout(100L, TimeUnit.NANOSECONDS);
  }

  @Test
  public void testStatementSetTransactionMode() {
    subject.statementSetTransactionMode(TransactionMode.READ_ONLY_TRANSACTION);
    verify(connection).setTransactionMode(TransactionMode.READ_ONLY_TRANSACTION);
    subject.statementSetTransactionMode(TransactionMode.READ_WRITE_TRANSACTION);
    verify(connection).setTransactionMode(TransactionMode.READ_WRITE_TRANSACTION);
  }

  @Test
  public void testStatementSetPgTransactionMode() {
    subject.statementSetPgTransactionMode(PgTransactionMode.READ_ONLY_TRANSACTION);
    verify(connection).setTransactionMode(TransactionMode.READ_ONLY_TRANSACTION);
    subject.statementSetPgTransactionMode(PgTransactionMode.READ_WRITE_TRANSACTION);
    verify(connection).setTransactionMode(TransactionMode.READ_WRITE_TRANSACTION);
  }

  @Test
  public void testStatementSetPgTransactionModeNoOp() {
    subject.statementSetPgTransactionMode(PgTransactionMode.ISOLATION_LEVEL_DEFAULT);
    subject.statementSetPgTransactionMode(PgTransactionMode.ISOLATION_LEVEL_SERIALIZABLE);
    verify(connection, never()).setTransactionMode(TransactionMode.READ_ONLY_TRANSACTION);
    verify(connection, never()).setTransactionMode(TransactionMode.READ_WRITE_TRANSACTION);
  }

  private ResultSet getMockResultSetForAnalyseQuery(){
    ResultSet rs = new ResultSet() {

      @Override
      public boolean next() throws SpannerException {
        return false;
      }

      @Override
      public Struct getCurrentRowAsStruct() {
        return null;
      }

      @Override
      public void close() {

      }

      @Nullable
      @Override
      public ResultSetStats getStats() {
        return null;
      }

      @Override
      public Type getType() {
        return null;
      }

      @Override
      public int getColumnCount() {
        return 1;
      }

      @Override
      public int getColumnIndex(String columnName) {
        if(columnName.equalsIgnoreCase("query plan")) return 0;
        return -1;

      }

      @Override
      public Type getColumnType(int columnIndex) {
        return null;
      }

      @Override
      public Type getColumnType(String columnName) {
        return null;
      }

      @Override
      public boolean isNull(int columnIndex) {
        return false;
      }

      @Override
      public boolean isNull(String columnName) {
        return false;
      }

      @Override
      public boolean getBoolean(int columnIndex) {
        return false;
      }

      @Override
      public boolean getBoolean(String columnName) {
        return false;
      }

      @Override
      public long getLong(int columnIndex) {
        return 0;
      }

      @Override
      public long getLong(String columnName) {
        return 0;
      }

      @Override
      public double getDouble(int columnIndex) {
        return 0;
      }

      @Override
      public double getDouble(String columnName) {
        return 0;
      }

      @Override
      public BigDecimal getBigDecimal(int columnIndex) {
        return null;
      }

      @Override
      public BigDecimal getBigDecimal(String columnName) {
        return null;
      }

      @Override
      public String getString(int columnIndex) {
        return null;
      }

      @Override
      public String getString(String columnName) {
        return null;
      }

      @Override
      public ByteArray getBytes(int columnIndex) {
        return null;
      }

      @Override
      public ByteArray getBytes(String columnName) {
        return null;
      }

      @Override
      public Timestamp getTimestamp(int columnIndex) {
        return null;
      }

      @Override
      public Timestamp getTimestamp(String columnName) {
        return null;
      }

      @Override
      public Date getDate(int columnIndex) {
        return null;
      }

      @Override
      public Date getDate(String columnName) {
        return null;
      }

      @Override
      public boolean[] getBooleanArray(int columnIndex) {
        return new boolean[0];
      }

      @Override
      public boolean[] getBooleanArray(String columnName) {
        return new boolean[0];
      }

      @Override
      public List<Boolean> getBooleanList(int columnIndex) {
        return null;
      }

      @Override
      public List<Boolean> getBooleanList(String columnName) {
        return null;
      }

      @Override
      public long[] getLongArray(int columnIndex) {
        return new long[0];
      }

      @Override
      public long[] getLongArray(String columnName) {
        return new long[0];
      }

      @Override
      public List<Long> getLongList(int columnIndex) {
        return null;
      }

      @Override
      public List<Long> getLongList(String columnName) {
        return null;
      }

      @Override
      public double[] getDoubleArray(int columnIndex) {
        return new double[0];
      }

      @Override
      public double[] getDoubleArray(String columnName) {
        return new double[0];
      }

      @Override
      public List<Double> getDoubleList(int columnIndex) {
        return null;
      }

      @Override
      public List<Double> getDoubleList(String columnName) {
        return null;
      }

      @Override
      public List<BigDecimal> getBigDecimalList(int columnIndex) {
        return null;
      }

      @Override
      public List<BigDecimal> getBigDecimalList(String columnName) {
        return null;
      }

      @Override
      public List<String> getStringList(int columnIndex) {
        return null;
      }

      @Override
      public List<String> getStringList(String columnName) {
        return null;
      }

      @Override
      public List<ByteArray> getBytesList(int columnIndex) {
        return null;
      }

      @Override
      public List<ByteArray> getBytesList(String columnName) {
        return null;
      }

      @Override
      public List<Timestamp> getTimestampList(int columnIndex) {
        return null;
      }

      @Override
      public List<Timestamp> getTimestampList(String columnName) {
        return null;
      }

      @Override
      public List<Date> getDateList(int columnIndex) {
        return null;
      }

      @Override
      public List<Date> getDateList(String columnName) {
        return null;
      }

      @Override
      public List<Struct> getStructList(int columnIndex) {
        return null;
      }

      @Override
      public List<Struct> getStructList(String columnName) {
        return null;
      }
    };
    return rs;

  }
  @Test
  public void testStatementExplain(){
    String sql = "verbose select * from table";
    try {
      ResultSet rs = subject.statementExplain(sql);
      Assert.assertEquals(true, false);
    }
    catch (SpannerException e){
      Assert.assertEquals(e.getErrorCode(), ErrorCode.UNIMPLEMENTED);
    }

    sql = "select * from table";
    when(connection.analyzeQuery(any(), any())).thenReturn(getMockResultSetForAnalyseQuery());

    ResultSet rs = subject.statementExplain(sql);

    Assert.assertEquals(1, rs.getColumnCount());
    Assert.assertEquals(0, rs.getColumnIndex("query plan"));
    Assert.assertEquals(-1, rs.getColumnIndex("random column"));

  }
}
