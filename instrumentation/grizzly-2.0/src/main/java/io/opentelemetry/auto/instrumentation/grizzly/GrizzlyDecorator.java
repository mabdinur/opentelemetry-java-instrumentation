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

package io.opentelemetry.auto.instrumentation.grizzly;

import static io.opentelemetry.auto.bootstrap.instrumentation.decorator.HttpServerTracer.CONTEXT_ATTRIBUTE;
import static io.opentelemetry.trace.Span.Kind.SERVER;

import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.auto.bootstrap.instrumentation.decorator.HttpServerDecorator;
import io.opentelemetry.context.ContextUtils;
import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.TracingContextUtils;
import java.net.URI;
import java.net.URISyntaxException;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;

public class GrizzlyDecorator
    extends HttpServerDecorator<HttpRequestPacket, HttpRequestPacket, HttpResponsePacket> {

  public static final GrizzlyDecorator DECORATE = new GrizzlyDecorator();

  public static final Tracer TRACER =
      OpenTelemetry.getTracerProvider().get("io.opentelemetry.auto.grizzly-http-2.3");

  @Override
  protected String method(final HttpRequestPacket httpRequest) {
    return httpRequest.getMethod().getMethodString();
  }

  @Override
  protected URI url(final HttpRequestPacket httpRequest) throws URISyntaxException {
    return new URI(
        (httpRequest.isSecure() ? "https://" : "http://")
            + httpRequest.getRemoteHost()
            + ":"
            + httpRequest.getLocalPort()
            + httpRequest.getRequestURI()
            + (httpRequest.getQueryString() != null ? "?" + httpRequest.getQueryString() : ""));
  }

  @Override
  protected String peerHostIP(final HttpRequestPacket httpRequest) {
    return httpRequest.getLocalHost();
  }

  @Override
  protected Integer peerPort(final HttpRequestPacket httpRequest) {
    return httpRequest.getLocalPort();
  }

  @Override
  protected Integer status(final HttpResponsePacket httpResponse) {
    return httpResponse.getStatus();
  }

  public static void onHttpServerFilterPrepareResponseExit(
      final FilterChainContext ctx, final HttpResponsePacket responsePacket) {
    final Context context = (Context) ctx.getAttributes().getAttribute(CONTEXT_ATTRIBUTE);
    Span span = TracingContextUtils.getSpan(context);
    DECORATE.onResponse(span, responsePacket);
    span.end();
    ctx.getAttributes().removeAttribute(CONTEXT_ATTRIBUTE);
    ctx.getAttributes().removeAttribute(RESPONSE_ATTRIBUTE);
  }

  public static void onHttpCodecFilterExit(
      final FilterChainContext ctx, final HttpHeader httpHeader) {
    // only create a span if there isn't another one attached to the current ctx
    // and if the httpHeader has been parsed into a HttpRequestPacket
    if (ctx.getAttributes().getAttribute(CONTEXT_ATTRIBUTE) != null
        || !(httpHeader instanceof HttpRequestPacket)) {
      return;
    }
    final HttpRequestPacket httpRequest = (HttpRequestPacket) httpHeader;
    final HttpResponsePacket httpResponse = httpRequest.getResponse();
    final Span span =
        TRACER
            .spanBuilder(DECORATE.spanNameForRequest(httpRequest))
            .setSpanKind(SERVER)
            .setParent(extract(httpHeader, ExtractAdapter.GETTER))
            .startSpan();
    Context newContext = TracingContextUtils.withSpan(span, Context.current());
    try (final Scope ignored = ContextUtils.withScopedContext(newContext)) {
      DECORATE.afterStart(span);
      ctx.getAttributes().setAttribute(CONTEXT_ATTRIBUTE, newContext);
      ctx.getAttributes().setAttribute(RESPONSE_ATTRIBUTE, httpResponse);
      DECORATE.onConnection(span, httpRequest);
      DECORATE.onRequest(span, httpRequest);
    }
  }

  public static void onFilterChainFail(final FilterChainContext ctx, final Throwable throwable) {
    final Context context = (Context) ctx.getAttributes().getAttribute(CONTEXT_ATTRIBUTE);
    if (null != context) {
      Span span = TracingContextUtils.getSpan(context);
      DECORATE.onError(span, throwable);
      DECORATE.beforeFinish(span);
      span.end();
    }
    ctx.getAttributes().removeAttribute(CONTEXT_ATTRIBUTE);
    ctx.getAttributes().removeAttribute(RESPONSE_ATTRIBUTE);
  }
}
