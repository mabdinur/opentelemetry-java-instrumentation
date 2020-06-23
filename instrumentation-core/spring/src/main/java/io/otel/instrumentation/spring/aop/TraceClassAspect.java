package io.otel.instrumentation.spring.aop;

import java.util.logging.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import io.opentelemetry.trace.Tracer;
import io.otel.instrumentation.spring.annotations.TraceClass;

@Aspect
@Configuration
public class TraceClassAspect {

  public static final Logger LOG = Logger.getLogger(TraceClassAspect.class.getName());

  @Autowired private Tracer tracer;

  @Pointcut("within(@io.otel.instrumentation.spring.annotations.TraceClass *)")
  public void beanAnnotatedWithTracedClass() {}

  @Pointcut("execution(public * *(..))")
  public void publicMethod() {}

  @Pointcut("publicMethod() && beanAnnotatedWithTracedClass()")
  public void publicMethodInsideAClassMarkedWithAtTracedClass() {}

  @Around("publicMethodInsideAClassMarkedWithAtTracedClass()")
  public Object tracedClass(final ProceedingJoinPoint pjp) throws Throwable {
    Class<? extends Object> annotatedClass = pjp.getTarget().getClass();
    String className = annotatedClass.getName();
    TraceClass traceClass = annotatedClass.getAnnotation(TraceClass.class);

    LOG.info("Traced Span " + className);
    return Handler.proceed(pjp, tracer, className, traceClass.isEvent());
  }
}

