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
package io.otel.instrumentation.spring.aop;

import io.opentelemetry.trace.Tracer;
import io.otel.instrumentation.spring.annotations.TracedClass;
import io.otel.instrumentation.spring.annotations.TracedMethod;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
public class OTaspect {

  public static final Logger LOG = Logger.getLogger(OTaspect.class.getName());

  @Autowired private Tracer tracer;

  @Pointcut("within(@io.otel.instrumentation.spring.annotations.TracedClass *)")
  public void beanAnnotatedWithTracedClass() {}

  @Pointcut("execution(public * *(..))")
  public void publicMethod() {}

  @Pointcut("publicMethod() && beanAnnotatedWithTracedClass()")
  public void publicMethodInsideAClassMarkedWithAtTracedClass() {}

  @Around("publicMethodInsideAClassMarkedWithAtTracedClass()")
  public Object tracedClass(final ProceedingJoinPoint pjp) throws Throwable {
    Class<? extends Object> annotatedClass = pjp.getTarget().getClass();
    String className = annotatedClass.getName();
    TracedClass tracedClass = annotatedClass.getAnnotation(TracedClass.class);

    LOG.info("Traced Span " + className);
    return Handler.proceed(pjp, tracer, className, tracedClass.isEvent());
  }

  @Around("@annotation(io.otel.instrumentation.spring.annotations.TracedMethod)")
  public Object tracedMethod(final ProceedingJoinPoint pjp) throws Throwable {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    Method method = signature.getMethod();
    TracedMethod tracedMethod = method.getAnnotation(TracedMethod.class);

    String methodName = tracedMethod.name();
    if (methodName.isEmpty()) {
      methodName = method.getName();
    }

    LOG.info("Traced Span " + methodName);

    return Handler.proceed(pjp, tracer, methodName, tracedMethod.isEvent());
  }
}
