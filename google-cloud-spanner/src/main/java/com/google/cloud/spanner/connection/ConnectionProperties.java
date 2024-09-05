/*
 * Copyright 2024 Google LLC
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

import static com.google.cloud.spanner.connection.ConnectionOptions.AUTOCOMMIT_PROPERTY_NAME;
import static com.google.cloud.spanner.connection.ConnectionOptions.DEFAULT_AUTOCOMMIT;
import static com.google.cloud.spanner.connection.ConnectionOptions.DEFAULT_READONLY;
import static com.google.cloud.spanner.connection.ConnectionOptions.DEFAULT_RETRY_ABORTS_INTERNALLY;
import static com.google.cloud.spanner.connection.ConnectionOptions.READONLY_PROPERTY_NAME;
import static com.google.cloud.spanner.connection.ConnectionOptions.RETRY_ABORTS_INTERNALLY_PROPERTY_NAME;
import static com.google.cloud.spanner.connection.ConnectionProperty.castProperty;

import com.google.cloud.spanner.connection.ClientSideStatementValueConverters.AutocommitDmlModeConverter;
import com.google.cloud.spanner.connection.ClientSideStatementValueConverters.BooleanConverter;
import com.google.cloud.spanner.connection.ClientSideStatementValueConverters.ConnectionStateTypeConverter;
import com.google.cloud.spanner.connection.ConnectionProperty.Context;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * Utility class that defines all known connection properties. This class will eventually replace
 * the list of {@link com.google.cloud.spanner.connection.ConnectionOptions.ConnectionProperty} in
 * {@link ConnectionOptions}.
 */
class ConnectionProperties {
  private static final ImmutableMap.Builder<String, ConnectionProperty<?>>
      CONNECTION_PROPERTIES_BUILDER = ImmutableMap.builder();

  static final ConnectionProperty<ConnectionState.Type> CONNECTION_STATE_TYPE =
      create(
          "connectionStateType",
          "The type of connection state to use for this connection. Can only be set at start up. "
              + "If no value is set, then the database dialect default will be used, "
              + "which is NON_TRANSACTIONAL for GoogleSQL and TRANSACTIONAL for PostgreSQL.",
          null,
          ConnectionStateTypeConverter.INSTANCE,
          Context.STARTUP);
  static final ConnectionProperty<Boolean> AUTOCOMMIT =
      create(
          AUTOCOMMIT_PROPERTY_NAME,
          "Should the connection start in autocommit (true/false)",
          DEFAULT_AUTOCOMMIT,
          BooleanConverter.INSTANCE,
          Context.USER);
  static final ConnectionProperty<Boolean> READONLY =
      create(
          READONLY_PROPERTY_NAME,
          "Should the connection start in read-only mode (true/false)",
          DEFAULT_READONLY,
          BooleanConverter.INSTANCE,
          Context.USER);
  static final ConnectionProperty<AutocommitDmlMode> AUTOCOMMIT_DML_MODE =
      create(
          "autocommit_dml_mode",
          "Should the connection automatically retry Aborted errors (true/false)",
          AutocommitDmlMode.TRANSACTIONAL,
          AutocommitDmlModeConverter.INSTANCE,
          Context.USER);
  static final ConnectionProperty<Boolean> RETRY_ABORTS_INTERNALLY =
      create(
          RETRY_ABORTS_INTERNALLY_PROPERTY_NAME,
          "Should the connection automatically retry Aborted errors (true/false)",
          DEFAULT_RETRY_ABORTS_INTERNALLY,
          BooleanConverter.INSTANCE,
          Context.USER);

  static final Map<String, ConnectionProperty<?>> CONNECTION_PROPERTIES =
      CONNECTION_PROPERTIES_BUILDER.build();

  /** Utility method for creating a new core {@link ConnectionProperty}. */
  private static <T> ConnectionProperty<T> create(
      String name,
      String description,
      T defaultValue,
      ClientSideStatementValueConverter<T> converter,
      Context context) {
    ConnectionProperty<T> property =
        ConnectionProperty.create(name, description, defaultValue, converter, context);
    CONNECTION_PROPERTIES_BUILDER.put(property.getKey(), property);
    return property;
  }

  /** Parse the connection properties that can be found in the given connection URL. */
  static ImmutableMap<String, ConnectionPropertyValue<?>> parseValues(String url) {
    ImmutableMap.Builder<String, ConnectionPropertyValue<?>> builder = ImmutableMap.builder();
    for (ConnectionProperty<?> property : CONNECTION_PROPERTIES.values()) {
      ConnectionPropertyValue<?> value = parseValue(castProperty(property), url);
      if (value != null) {
        builder.put(property.getKey(), value);
      }
    }
    return builder.build();
  }

  /**
   * Parse and convert the value of the specific connection property from a connection URL (e.g.
   * readonly=true).
   */
  private static <T> ConnectionPropertyValue<T> parseValue(
      ConnectionProperty<T> property, String url) {
    String stringValue = ConnectionOptions.parseUriProperty(url, property.getKey());
    return property.convert(stringValue);
  }

  /** This class should not be instantiated. */
  private ConnectionProperties() {}
}
