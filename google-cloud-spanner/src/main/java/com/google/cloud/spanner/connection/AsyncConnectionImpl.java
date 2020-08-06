package com.google.cloud.spanner.connection;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AsyncResultSet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.ReadContext.QueryAnalyzeMode;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TimestampBound;
import java.util.Iterator;

public class AsyncConnectionImpl implements AsyncConnection {

  @Override
  public void close() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean isClosed() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setAutocommit(boolean autocommit) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean isAutocommit() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setReadOnly(boolean readOnly) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean isReadOnly() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public ApiFuture<Void> beginTransactionAsync() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setTransactionMode(TransactionMode transactionMode) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public TransactionMode getTransactionMode() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isRetryAbortsInternally() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setRetryAbortsInternally(boolean retryAbortsInternally) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addTransactionRetryListener(TransactionRetryListener listener) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean removeTransactionRetryListener(TransactionRetryListener listener) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Iterator<TransactionRetryListener> getTransactionRetryListeners() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setAutocommitDmlMode(AutocommitDmlMode mode) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public AutocommitDmlMode getAutocommitDmlMode() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setReadOnlyStaleness(TimestampBound staleness) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public TimestampBound getReadOnlyStaleness() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setOptimizerVersion(String optimizerVersion) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public String getOptimizerVersion() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiFuture<Void> commitAsync() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiFuture<Void> rollbackAsync() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isInTransaction() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isTransactionStarted() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Timestamp getReadTimestamp() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Timestamp getCommitTimestamp() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void startBatchDdl() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void startBatchDml() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public long[] runBatch() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void abortBatch() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean isDdlBatchActive() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isDmlBatchActive() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public AsyncStatementResult executeAsync(Statement statement) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AsyncResultSet executeQueryAsync(Statement query, QueryOption... options) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AsyncResultSet analyzeQueryAsync(Statement query, QueryAnalyzeMode queryMode) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiFuture<Long> executeUpdateAsync(Statement update) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApiFuture<long[]> executeBatchUpdateAsync(Iterable<Statement> updates) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void bufferedWrite(Mutation mutation) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void bufferedWrite(Iterable<Mutation> mutations) {
    // TODO Auto-generated method stub
    
  }}
