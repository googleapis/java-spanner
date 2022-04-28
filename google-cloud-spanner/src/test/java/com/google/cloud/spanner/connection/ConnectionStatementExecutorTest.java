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

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Dialect;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.connection.ClientSideStatementImpl.CompileException;
import com.google.cloud.spanner.connection.ClientSideStatementValueConverters.ExplainCommandConverter;
import com.google.protobuf.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

  private void assertExplain(ResultSet rs){
    int countRows = 0;
    while (rs.next()) {
      ++countRows;
    }

    Assert.assertEquals(1, countRows);
    Assert.assertEquals(1, rs.getColumnCount());
    Assert.assertEquals(1, rs.getColumnIndex("query plan"));
    Assert.assertEquals(0, rs.getColumnIndex("random column"));

  }

  private void assertExplainAnalyse(ResultSet rs){
    int countRows = 0;
    while (rs.next()) {
      ++countRows;
    }

    Assert.assertEquals(3, countRows);
    Assert.assertEquals(1, rs.getColumnCount());
    Assert.assertEquals(1, rs.getColumnIndex("query plan"));
    Assert.assertEquals(0, rs.getColumnIndex("random column"));


  }

  @Test
  public void testStatementExplain() {

    ExplainCommandConverter converter = new ExplainCommandConverter();

    /*Test for proper EXPLAIN*/
    String sql = converter.convert("explain select * from table");

    ResultSet expectedRs = mock(ResultSet.class);
    when(expectedRs.next()).thenReturn(true, false);
    when(expectedRs.getColumnCount()).thenReturn(1);
    when(expectedRs.getColumnIndex("query plan")).thenReturn(1);

    when(connection.analyzeQuery(any(), any())).thenReturn(expectedRs);

    ResultSet rs = subject.statementExplain(sql).getResultSet();

    assertExplain(rs);
    /*Test for EXPLAIN ANALYSE*/
    sql = converter.convert("explain analyse select * from table1");

    when(expectedRs.next()).thenReturn(true, true, true, false);

    rs = subject.statementExplain(sql).getResultSet();

    assertExplainAnalyse(rs);

    /*Test for Unsupported Option*/
    String sql2 = converter.convert("explain verbose select * from table");
    Assert.assertThrows(SpannerException.class, () -> subject.statementExplain(sql2));

    /*Test for explain with extra spaces, tab, \n*/

    sql = converter.convert("    select \n * from   \t table1");

    rs = subject.statementExplain(sql).getResultSet();
    when(expectedRs.next()).thenReturn(true, false);

    assertExplain(rs);

    /*Test for explain analyse with extra spaces, tab, \n*/

    sql = converter.convert("   analyse \n select  * from  \t table1");

    rs = subject.statementExplain(sql).getResultSet();
    when(expectedRs.next()).thenReturn(true, true, true, false);

    assertExplainAnalyse(rs);

    /*Test for explain with two words*/

    sql = converter.convert("explain foo");

    rs = subject.statementExplain(sql).getResultSet();
    when(expectedRs.next()).thenReturn(true, false);

    assertExplain(rs);

    /*Test for explain analyse with two words*/

    sql = converter.convert("explain analyse foo");

    rs = subject.statementExplain(sql).getResultSet();
    when(expectedRs.next()).thenReturn(true, true, true, false);

    assertExplainAnalyse(rs);



  }
}
