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

import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import io.opentelemetry.trace.Tracer;
import io.otel.instrumentation.spring.annotations.TraceClass;
import io.otel.instrumentation.spring.annotations.TraceMethod;

@Aspect
@Configuration
public class TraceMethodAspect {

  public static final Logger LOG = Logger.getLogger(TraceMethodAspect.class.getName());

  @Autowired private Tracer tracer;

  @Around("@annotation(io.otel.instrumentation.spring.annotations.TraceMethod)")
  public Object tracedMethod(final ProceedingJoinPoint pjp) throws Throwable {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    Method method = signature.getMethod();
    TraceMethod traceMethod = method.getAnnotation(TraceMethod.class);

    String methodName = traceMethod.name();
    if (methodName.isEmpty()) {
      methodName = method.getName();
    }

    LOG.info("Traced Span " + methodName);

    return Handler.proceed(pjp, tracer, methodName, traceMethod.isEvent());
  }
}
