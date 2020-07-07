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

package com.google.cloud.spanner;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.util.List;

/** Forwarding implements of StructReader */
public class ForwardingStructReader implements StructReader {

  private Supplier<? extends StructReader> delegate;

  public ForwardingStructReader(StructReader delegate) {
    this.delegate = Suppliers.ofInstance(Preconditions.checkNotNull(delegate));
  }

  public ForwardingStructReader(Supplier<? extends StructReader> delegate) {
    this.delegate = Preconditions.checkNotNull(delegate);
  }

  /**
   * Replaces the underlying {@link StructReader}. It is the responsibility of the caller to ensure
   * that the new delegate has the same properties and is in the same state as the original
   * delegate. This method can be used if the underlying delegate needs to be replaced after a
   * session or transaction needed to be restarted after the {@link StructReader} had already been
   * returned to the user.
   */
  void replaceDelegate(StructReader newDelegate) {
    this.delegate = Suppliers.ofInstance(Preconditions.checkNotNull(newDelegate));
  }

  /**
   * Called before each forwarding call to allow sub classes to do additional state checking. Sub
   * classes should throw an {@link Exception} if the current state is not valid for reading data
   * from this {@link ForwardingStructReader}. The default implementation does nothing.
   */
  protected void checkValidState() {}

  @Override
  public Type getType() {
    checkValidState();
    return delegate.get().getType();
  }

  @Override
  public int getColumnCount() {
    checkValidState();
    return delegate.get().getColumnCount();
  }

  @Override
  public int getColumnIndex(String columnName) {
    checkValidState();
    return delegate.get().getColumnIndex(columnName);
  }

  @Override
  public Type getColumnType(int columnIndex) {
    checkValidState();
    return delegate.get().getColumnType(columnIndex);
  }

  @Override
  public Type getColumnType(String columnName) {
    checkValidState();
    return delegate.get().getColumnType(columnName);
  }

  @Override
  public boolean isNull(int columnIndex) {
    checkValidState();
    return delegate.get().isNull(columnIndex);
  }

  @Override
  public boolean isNull(String columnName) {
    checkValidState();
    return delegate.get().isNull(columnName);
  }

  @Override
  public boolean getBoolean(int columnIndex) {
    checkValidState();
    return delegate.get().getBoolean(columnIndex);
  }

  @Override
  public boolean getBoolean(String columnName) {
    checkValidState();
    return delegate.get().getBoolean(columnName);
  }

  @Override
  public long getLong(int columnIndex) {
    checkValidState();
    return delegate.get().getLong(columnIndex);
  }

  @Override
  public long getLong(String columnName) {
    checkValidState();
    return delegate.get().getLong(columnName);
  }

  @Override
  public double getDouble(int columnIndex) {
    checkValidState();
    return delegate.get().getDouble(columnIndex);
  }

  @Override
  public double getDouble(String columnName) {
    checkValidState();
    return delegate.get().getDouble(columnName);
  }

  @Override
  public String getString(int columnIndex) {
    checkValidState();
    return delegate.get().getString(columnIndex);
  }

  @Override
  public String getString(String columnName) {
    checkValidState();
    return delegate.get().getString(columnName);
  }

  @Override
  public ByteArray getBytes(int columnIndex) {
    checkValidState();
    return delegate.get().getBytes(columnIndex);
  }

  @Override
  public ByteArray getBytes(String columnName) {
    checkValidState();
    return delegate.get().getBytes(columnName);
  }

  @Override
  public Timestamp getTimestamp(int columnIndex) {
    checkValidState();
    return delegate.get().getTimestamp(columnIndex);
  }

  @Override
  public Timestamp getTimestamp(String columnName) {
    checkValidState();
    return delegate.get().getTimestamp(columnName);
  }

  @Override
  public Date getDate(int columnIndex) {
    checkValidState();
    return delegate.get().getDate(columnIndex);
  }

  @Override
  public Date getDate(String columnName) {
    checkValidState();
    return delegate.get().getDate(columnName);
  }

  @Override
  public boolean[] getBooleanArray(int columnIndex) {
    checkValidState();
    return delegate.get().getBooleanArray(columnIndex);
  }

  @Override
  public boolean[] getBooleanArray(String columnName) {
    checkValidState();
    return delegate.get().getBooleanArray(columnName);
  }

  @Override
  public List<Boolean> getBooleanList(int columnIndex) {
    checkValidState();
    return delegate.get().getBooleanList(columnIndex);
  }

  @Override
  public List<Boolean> getBooleanList(String columnName) {
    checkValidState();
    return delegate.get().getBooleanList(columnName);
  }

  @Override
  public long[] getLongArray(int columnIndex) {
    checkValidState();
    return delegate.get().getLongArray(columnIndex);
  }

  @Override
  public long[] getLongArray(String columnName) {
    checkValidState();
    return delegate.get().getLongArray(columnName);
  }

  @Override
  public List<Long> getLongList(int columnIndex) {
    checkValidState();
    return delegate.get().getLongList(columnIndex);
  }

  @Override
  public List<Long> getLongList(String columnName) {
    checkValidState();
    return delegate.get().getLongList(columnName);
  }

  @Override
  public double[] getDoubleArray(int columnIndex) {
    checkValidState();
    return delegate.get().getDoubleArray(columnIndex);
  }

  @Override
  public double[] getDoubleArray(String columnName) {
    checkValidState();
    return delegate.get().getDoubleArray(columnName);
  }

  @Override
  public List<Double> getDoubleList(int columnIndex) {
    checkValidState();
    return delegate.get().getDoubleList(columnIndex);
  }

  @Override
  public List<Double> getDoubleList(String columnName) {
    checkValidState();
    return delegate.get().getDoubleList(columnName);
  }

  @Override
  public List<String> getStringList(int columnIndex) {
    checkValidState();
    return delegate.get().getStringList(columnIndex);
  }

  @Override
  public List<String> getStringList(String columnName) {
    checkValidState();
    return delegate.get().getStringList(columnName);
  }

  @Override
  public List<ByteArray> getBytesList(int columnIndex) {
    checkValidState();
    return delegate.get().getBytesList(columnIndex);
  }

  @Override
  public List<ByteArray> getBytesList(String columnName) {
    checkValidState();
    return delegate.get().getBytesList(columnName);
  }

  @Override
  public List<Timestamp> getTimestampList(int columnIndex) {
    checkValidState();
    return delegate.get().getTimestampList(columnIndex);
  }

  @Override
  public List<Timestamp> getTimestampList(String columnName) {
    checkValidState();
    return delegate.get().getTimestampList(columnName);
  }

  @Override
  public List<Date> getDateList(int columnIndex) {
    checkValidState();
    return delegate.get().getDateList(columnIndex);
  }

  @Override
  public List<Date> getDateList(String columnName) {
    checkValidState();
    return delegate.get().getDateList(columnName);
  }

  @Override
  public List<Struct> getStructList(int columnIndex) {
    checkValidState();
    return delegate.get().getStructList(columnIndex);
  }

  @Override
  public List<Struct> getStructList(String columnName) {
    checkValidState();
    return delegate.get().getStructList(columnName);
  }
}
