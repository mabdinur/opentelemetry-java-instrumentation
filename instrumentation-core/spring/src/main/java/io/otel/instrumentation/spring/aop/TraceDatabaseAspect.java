package io.otel.instrumentation.spring.aop;

import java.util.logging.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.beans.factory.annotation.Autowired;
import io.opentelemetry.trace.Tracer;

public class TraceDatabaseAspect {
  public static final Logger LOG = Logger.getLogger(TraceDatabaseAspect.class.getName());

  @Autowired private Tracer tracer;

  @Around("@annotation(io.otel.instrumentation.spring.annotations.TraceDatabase)")
  public Object tracedMethod(final ProceedingJoinPoint pjp) {
    return null;
  }
}
