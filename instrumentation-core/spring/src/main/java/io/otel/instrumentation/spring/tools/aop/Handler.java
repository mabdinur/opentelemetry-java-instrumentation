package io.otel.instrumentation.spring.tools.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;

public final class Handler {
  
  public static Object proceed(ProceedingJoinPoint pjp, Tracer tracer, String methodName,
      boolean isEvent) throws Throwable {
    if (isEvent) {
      return proceedEvent(pjp, tracer, methodName);
    }
    return proceedSpan(pjp, tracer, methodName);
  }

  static Object proceedSpan(ProceedingJoinPoint call, Tracer tracer, String spanName)
      throws Throwable {

    Span span = tracer.spanBuilder(spanName).startSpan();
    try {
      return call.proceed();
    } catch (Throwable t) {
      errorHandler(span, t);
      throw t;
    } finally {
      span.end();
    }
  }

  static Object proceedEvent(ProceedingJoinPoint call, Tracer tracer, String event)
      throws Throwable {

    Span span = tracer.getCurrentSpan();
    span.addEvent(event);
    try {
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
