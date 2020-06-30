/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentelemetry.spring.instrumentation.trace.aspects;

import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Inspired by:
 *
 * <p><a href=
 * "https://github.com/census-instrumentation/opencensus-java/blob/master/contrib/spring/src/main/java/io/opencensus/contrib/spring/aop/Handler.java">
 * opencensus-spring aspects </a>
 */
public final class AspectHandler {

  /**
   * Creates a trace event or wraps a JoinPoint in a span
   *
   * @return target method invocation or next aspect
   */
  public static Object proceed(
      ProceedingJoinPoint pjp, Tracer tracer, String methodName, boolean isEvent) throws Throwable {
    if (isEvent) {
      return proceedEvent(pjp, tracer, methodName);
    }
    return proceedSpan(pjp, tracer, methodName);
  }

  private static Object proceedSpan(ProceedingJoinPoint call, Tracer tracer, String spanName)
      throws Throwable {

    Span span = tracer.spanBuilder(spanName).startSpan();
    try (Scope scope = tracer.withSpan(span)) {
      return call.proceed();
    } catch (Throwable t) {
      errorHandler(span, t);
      throw t;
    } finally {
      span.end();
    }
  }

  private static Object proceedEvent(ProceedingJoinPoint call, Tracer tracer, String event)
      throws Throwable {

    Span span = tracer.getCurrentSpan();
    span.addEvent(event);
    try (Scope scope = tracer.withSpan(span)) {
      return call.proceed();
    } catch (Throwable t) {
      errorHandler(span, t);
      throw t;
    }
  }

  private static void errorHandler(Span span, Throwable t) {
    String message = t.getMessage();
    span.addEvent(message);
    span.setAttribute("error", true);
    span.setStatus(Status.UNKNOWN);
  }
}
