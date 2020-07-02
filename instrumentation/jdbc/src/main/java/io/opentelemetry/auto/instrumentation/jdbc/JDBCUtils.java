/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.auto.instrumentation.jdbc;

import io.opentelemetry.auto.bootstrap.ExceptionLogger;
import io.opentelemetry.auto.config.Config;
import io.opentelemetry.auto.instrumentation.jdbc.normalizer.SqlNormalizer;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;

public abstract class JDBCUtils {
  private static Field c3poField = null;

  /**
   * @param statement
   * @return the unwrapped connection or null if exception was thrown.
   */
  public static Connection connectionFromStatement(final Statement statement) {
    Connection connection;
    try {
      connection = statement.getConnection();

      if (c3poField != null) {
        if (connection.getClass().getName().equals("com.mchange.v2.c3p0.impl.NewProxyConnection")) {
          return (Connection) c3poField.get(connection);
        }
      }

      try {
        // unwrap the connection to cache the underlying actual connection and to not cache proxy
        // objects
        if (connection.isWrapperFor(Connection.class)) {
          connection = connection.unwrap(Connection.class);
        }
      } catch (final Exception | AbstractMethodError e) {
        if (connection != null) {
          // Attempt to work around c3po delegating to an connection that doesn't support
          // unwrapping.
          final Class<? extends Connection> connectionClass = connection.getClass();
          if (connectionClass.getName().equals("com.mchange.v2.c3p0.impl.NewProxyConnection")) {
            final Field inner = connectionClass.getDeclaredField("inner");
            inner.setAccessible(true);
            c3poField = inner;
            return (Connection) c3poField.get(connection);
          }
        }

        // perhaps wrapping isn't supported?
        // ex: org.h2.jdbc.JdbcConnection v1.3.175
        // or: jdts.jdbc which always throws `AbstractMethodError` (at least up to version 1.3)
        // Stick with original connection.
      }
    } catch (final Throwable e) {
      // Had some problem getting the connection.
      ExceptionLogger.LOGGER.debug("Could not get connection for StatementAdvice", e);
      return null;
    }
    return connection;
  }

  /** @return null if the sql could not be normalized for any reason */
  public static String normalizeSql(String sql) {
    if (!Config.get().isSqlNormalizerEnabled()) {
      return sql;
    }
    try {
      return SqlNormalizer.normalize(sql);
    } catch (Exception e) {
      ExceptionLogger.LOGGER.debug("Could not normalize sql", e);
      return null;
    }
  }
}
