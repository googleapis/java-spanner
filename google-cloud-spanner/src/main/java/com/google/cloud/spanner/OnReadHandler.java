package com.google.cloud.spanner;

public interface OnReadHandler {

  void handle(ResultSet resultSet, TransactionContext transaction);
}
