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

import static io.opentelemetry.context.ContextUtils.withScopedContext;
import static io.opentelemetry.trace.Span.Kind.CLIENT;
import static io.opentelemetry.trace.TracingContextUtils.withSpan;

import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.auto.instrumentation.api.MoreAttributes;
import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.attributes.SemanticAttributes;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public abstract class DatabaseClientTracer<CONNECTION, QUERY> {
  // Keeps track of the client span for the current trace.
  private static final Context.Key<Span> CONTEXT_CLIENT_SPAN_KEY =
      Context.key("opentelemetry-trace-client-span-key");

  private static final String DB_QUERY = "DB Query";

  protected final Tracer tracer;

  public DatabaseClientTracer() {
    tracer = OpenTelemetry.getTracerProvider().get(getInstrumentationName(), getVersion());
  }

  public Span startSpan(CONNECTION connection, QUERY query) {
    String normalizedQuery = normalizeQuery(query);

    final Span span =
        tracer
            .spanBuilder(spanName(normalizedQuery))
            .setSpanKind(CLIENT)
            .setAttribute(SemanticAttributes.DB_TYPE.key(), dbType())
            .startSpan();

    if (connection != null) {
      onConnection(span, connection);
      onPeerConnection(span, connection);
    }
    onStatement(span, normalizedQuery);

    return span;
  }

  /**
   * Creates new scoped context with the given span.
   *
   * <p>Attaches new context to the request to avoid creating duplicate client spans.
   */
  public Scope startScope(Span span) {
    // TODO we could do this in one go, but TracingContextUtils.CONTEXT_SPAN_KEY is private
    Context clientSpanContext = Context.current().withValue(CONTEXT_CLIENT_SPAN_KEY, span);
    Context newContext = withSpan(span, clientSpanContext);
    return withScopedContext(newContext);
  }

  public Span getCurrentSpan() {
    return tracer.getCurrentSpan();
  }

  public Span getClientSpan() {
    final Context context = Context.current();
    return CONTEXT_CLIENT_SPAN_KEY.get(context);
  }

  // TODO make abstract when implemented in all subclasses
  protected String getInstrumentationName() {
    return null;
  }

  private String getVersion() {
    return null;
  }

  public void end(Span span) {
    span.end();
  }

  public void endExceptionally(Span span, Throwable throwable) {
    onError(span, throwable);
    end(span);
  }

  protected Span afterStart(final Span span) {
    assert span != null;
    span.setAttribute(SemanticAttributes.DB_TYPE.key(), dbType());
    return span;
  }

  /** This should be called when the connection is being used, not when it's created. */
  protected Span onConnection(final Span span, final CONNECTION connection) {
    span.setAttribute(SemanticAttributes.DB_USER.key(), dbUser(connection));
    span.setAttribute(SemanticAttributes.DB_INSTANCE.key(), dbInstance(connection));
    span.setAttribute(SemanticAttributes.DB_URL.key(), dbUrl(connection));
    return span;
  }

  protected Span onError(final Span span, final Throwable throwable) {
    assert span != null;
    if (throwable != null) {
      span.setStatus(Status.UNKNOWN);
      addThrowable(
          span, throwable instanceof ExecutionException ? throwable.getCause() : throwable);
    }
    return span;
  }

  protected static void addThrowable(final Span span, final Throwable throwable) {
    span.setAttribute(MoreAttributes.ERROR_MSG, throwable.getMessage());
    span.setAttribute(MoreAttributes.ERROR_TYPE, throwable.getClass().getName());

    final StringWriter errorString = new StringWriter();
    throwable.printStackTrace(new PrintWriter(errorString));
    span.setAttribute(MoreAttributes.ERROR_STACK, errorString.toString());
  }

  protected void onPeerConnection(Span span, final CONNECTION connection) {
    onPeerConnection(span, peerAddress(connection));
  }

  protected void onPeerConnection(final Span span, final InetSocketAddress remoteConnection) {
    if (remoteConnection != null) {
      onPeerConnection(span, remoteConnection.getAddress());
      span.setAttribute(SemanticAttributes.NET_PEER_PORT.key(), remoteConnection.getPort());
    }
  }

  protected void onPeerConnection(final Span span, final InetAddress remoteAddress) {
    if (remoteAddress != null) {
      span.setAttribute(SemanticAttributes.NET_PEER_NAME.key(), remoteAddress.getHostName());
      span.setAttribute(SemanticAttributes.NET_PEER_IP.key(), remoteAddress.getHostAddress());
    }
  }

  protected void onStatement(final Span span, final String statement) {
    span.setAttribute(SemanticAttributes.DB_STATEMENT.key(), statement);
  }

  // TODO: "When it's impossible to get any meaningful representation of the span name, it can be
  // populated using the same value as db.instance" (c) spec
  protected String spanName(final String query) {
    return query == null ? DB_QUERY : query;
  }

  protected abstract String normalizeQuery(QUERY query);

  protected abstract String dbType();

  protected abstract String dbUser(CONNECTION connection);

  protected abstract String dbInstance(CONNECTION connection);

  // TODO make abstract after implementing in all subclasses
  protected String dbUrl(final CONNECTION connection) {
    return null;
  }

  protected abstract InetSocketAddress peerAddress(CONNECTION connection);
}
