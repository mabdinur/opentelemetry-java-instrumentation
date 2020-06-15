package io.opentelemetry.instrumentation.spring.aop;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import io.opentelemetry.instrumentation.spring.annotations.TracedClass;
import io.opentelemetry.instrumentation.spring.annotations.TracedMethod;
import io.opentelemetry.trace.Tracer;

@Aspect
@Component
public class OTaspect {

  public static final Logger LOG = Logger.getLogger(OTaspect.class.getName());

  @Autowired
  private Tracer tracer;

  @Around("@annotation(io.opentelemetry.instrumentation.spring.annotations.TracedMethod)")
  public Object tracedClass(final ProceedingJoinPoint pjp) throws Throwable {

    MethodSignature signature = (MethodSignature) pjp.getSignature();
    Method method = signature.getMethod();

    TracedMethod annotation = method.getAnnotation(TracedMethod.class);
    if (annotation == null) {
      return pjp.proceed();
    }

    String spanName = annotation.name();
    if (spanName.isEmpty()) {
      spanName = method.getName();
    }
    LOG.info("Traced Span " + spanName);
    return Handler.proceed(pjp, tracer, spanName, annotation.isEvent());
  }

  @Around("@annotation(io.opentelemetry.instrumentation.spring.annotations.TracedClass)")
  public Object tracedMethod(final ProceedingJoinPoint pjp, final TracedClass tracedClass)
      throws Throwable {

    if (tracedClass == null) {
      return pjp.proceed();
    }

    String methodName = getMethodName(pjp, tracedClass.name());

    LOG.info("Traced Span Class " + methodName);

    return Handler.proceed(pjp, tracer, methodName, tracedClass.isEvent());

  }

  private String getMethodName(final ProceedingJoinPoint pjp, String methodName) {
    if (methodName.isEmpty()) {
      MethodSignature signature = (MethodSignature) pjp.getSignature();
      Method method = signature.getMethod();
      methodName = method.getName();
    }
    return methodName;
  }

}
