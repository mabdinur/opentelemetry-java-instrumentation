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

package io.opentelemetry.auto.test.utils

import io.opentelemetry.OpenTelemetry
import io.opentelemetry.auto.bootstrap.instrumentation.decorator.BaseDecorator
import io.opentelemetry.auto.test.asserts.TraceAssert
import io.opentelemetry.context.Scope
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.trace.Span
import io.opentelemetry.trace.Tracer
import java.util.concurrent.Callable

import static io.opentelemetry.trace.TracingContextUtils.currentContextWith

class TraceUtils {

  private static final BaseDecorator DECORATE = new BaseDecorator() {
  }

  private static final Tracer TRACER = OpenTelemetry.getTracerProvider().get("io.opentelemetry.auto")

  static <T> T runUnderTrace(final String rootOperationName, final Callable<T> r) {
    try {
      final Span span = TRACER.spanBuilder(rootOperationName).startSpan()
      DECORATE.afterStart(span)

      Scope scope = currentContextWith(span)

      try {
        return r.call()
      } catch (final Exception e) {
        DECORATE.onError(span, e)
        throw e
      } finally {
        DECORATE.beforeFinish(span)
        span.end()
        scope.close()
      }
    } catch (Throwable t) {
      throw ExceptionUtils.sneakyThrow(t)
    }
  }

  static basicSpan(TraceAssert trace, int index, String operation, Object parentSpan = null, Throwable exception = null) {
    trace.span(index) {
      if (parentSpan == null) {
        parent()
      } else {
        childOf((SpanData) parentSpan)
      }
      operationName operation
      errored exception != null
      attributes {
        if (exception) {
          errorAttributes(exception.class, exception.message)
        }
      }
    }
  }
}
