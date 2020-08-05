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

package io.opentelemetry.instrumentation.springwebflux.client;

import static io.opentelemetry.instrumentation.springwebflux.client.SpringWebfluxHttpClientDecorator.DECORATE;
import static io.opentelemetry.instrumentation.springwebflux.client.SpringWebfluxHttpClientDecorator.TRACER;

import io.grpc.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import java.util.List;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public class WebClientTracingFilter implements ExchangeFilterFunction {

  private final Tracer tracer;

  public WebClientTracingFilter(Tracer tracer) {
    this.tracer = tracer;
  }

  public static void addFilter(final List<ExchangeFilterFunction> exchangeFilterFunctions) {
    addFilter(exchangeFilterFunctions, TRACER);
  }

  public static void addFilter(
      final List<ExchangeFilterFunction> exchangeFilterFunctions, Tracer tracer) {
    exchangeFilterFunctions.add(0, new WebClientTracingFilter(tracer));
  }

  @Override
  public Mono<ClientResponse> filter(final ClientRequest request, final ExchangeFunction next) {
    Span span = DECORATE.getOrCreateSpan(request, tracer);
    DECORATE.afterStart(span);

    try (Scope scope = tracer.withSpan(span)) {
      ClientRequest mutatedRequest =
          ClientRequest.from(request)
              .headers(httpHeaders -> DECORATE.inject(Context.current(), httpHeaders))
              .build();
      DECORATE.onRequest(span, mutatedRequest);

      return next.exchange(mutatedRequest)
          .doOnSuccessOrError(
              (clientResponse, throwable) -> {
                if (throwable != null) {
                  DECORATE.onError(span, throwable);
                } else {
                  DECORATE.onResponse(span, clientResponse);
                }
                DECORATE.beforeFinish(span);
                span.end();
              })
          .doOnCancel(
              () -> {
                DECORATE.onCancel(span);
                DECORATE.beforeFinish(span);
                span.end();
              });
    }
  }
}
