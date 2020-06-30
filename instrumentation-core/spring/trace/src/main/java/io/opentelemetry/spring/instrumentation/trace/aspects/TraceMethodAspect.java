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

import io.opentelemetry.spring.instrumentation.trace.annotations.TraceMethod;
import io.opentelemetry.trace.Tracer;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Uses AOP to add instrumentation to methods annotated with {@literal @}TraceMethod */
@Aspect
public final class TraceMethodAspect {

  public static final Logger LOG = LoggerFactory.getLogger(TraceMethodAspect.class.getName());
  private Tracer tracer;

  public TraceMethodAspect(Tracer tracer) {
    this.tracer = tracer;
  }

  @Around("@annotation(io.opentelemetry.spring.instrumentation.trace.annotations.TraceMethod)")
  public Object traceMethod(final ProceedingJoinPoint pjp) throws Throwable {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    Method method = signature.getMethod();
    TraceMethod traceMethod = method.getAnnotation(TraceMethod.class);

    String methodName = traceMethod.name();
    if (methodName.isEmpty()) {
      methodName = method.getName();
    }

    LOG.info("Traced Span " + methodName);

    return AspectHandler.proceed(pjp, tracer, methodName, traceMethod.isEvent());
  }
}
