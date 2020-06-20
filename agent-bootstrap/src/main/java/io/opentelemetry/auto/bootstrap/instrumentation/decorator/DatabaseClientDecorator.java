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
package io.opentelemetry.auto.bootstrap.instrumentation.decorator;

import io.opentelemetry.auto.instrumentation.api.Tags;
import io.opentelemetry.trace.Span;

/** @deprecated use {@link DatabaseClientTracer} instead. */
@Deprecated
public abstract class DatabaseClientDecorator<CONNECTION> extends ClientDecorator {

  protected abstract String dbType();

  protected abstract String dbUser(CONNECTION connection);

  protected abstract String dbInstance(CONNECTION connection);

  // TODO make abstract after implementing in all subclasses
  protected String dbUrl(final CONNECTION connection) {
    return null;
  }

  @Override
  public Span afterStart(final Span span) {
    assert span != null;
    span.setAttribute(Tags.DB_TYPE, dbType());
    return super.afterStart(span);
  }

  /** This should be called when the connection is being used, not when it's created. */
  public Span onConnection(final Span span, final CONNECTION connection) {
    assert span != null;
    if (connection != null) {
      span.setAttribute(Tags.DB_USER, dbUser(connection));
      span.setAttribute(Tags.DB_INSTANCE, dbInstance(connection));
      span.setAttribute(Tags.DB_URL, dbUrl(connection));
    }
    return span;
  }

  public Span onStatement(final Span span, final String statement) {
    assert span != null;
    span.setAttribute(Tags.DB_STATEMENT, statement);
    return span;
  }
}
