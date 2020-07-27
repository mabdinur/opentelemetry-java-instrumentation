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

package io.opentelemetry.instrumentation.spring.autoconfigure.logging.slf4j;

import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.TracingContextUtils;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;

@Aspect
public final class Slf4jMdcAspect {
  private static final String SPAN_ID = "spanId";
  private static final String TRACE_ID = "traceId";
  private static final String TRACE_FLAGS = "traceFlags";

  @After("execution(* io.opentelemetry.trace.Span.Builder.startSpan())")
  public void addMDCtoSpanStart() throws Throwable {    
    setSpanIds();
  }

  @Pointcut("execution(* io.opentelemetry.trace.Span.end(..))")
  public void addMDCtoSpanEnd() throws Throwable {
    MDC.remove(TRACE_ID);
    MDC.remove(SPAN_ID);
    MDC.remove(TRACE_FLAGS);
    
    setSpanIds();
  }

  private void setSpanIds() {
    Span currentSpan = TracingContextUtils.getCurrentSpan();
    if (!currentSpan.getContext().isValid()) {
      return;
    }

    SpanContext spanContext = currentSpan.getContext();
    MDC.put(TRACE_ID, spanContext.getTraceId().toLowerBase16());
    MDC.put(SPAN_ID, spanContext.getSpanId().toLowerBase16());
    MDC.put(TRACE_FLAGS, spanContext.getTraceFlags().toLowerBase16());
  }
}
