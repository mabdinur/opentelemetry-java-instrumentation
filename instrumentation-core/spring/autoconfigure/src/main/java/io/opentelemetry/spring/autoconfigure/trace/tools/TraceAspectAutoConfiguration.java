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
package io.opentelemetry.spring.autoconfigure.trace.tools;

import io.opentelemetry.spring.instrumentation.trace.aspects.TraceClassAspect;
import io.opentelemetry.spring.instrumentation.trace.aspects.TraceMethodAspect;
import io.opentelemetry.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures TraceClassAspect and TraceMethodAspect beans <br>
 * Enables aspects for: <br>
 * - io.opentelemetry.spring.instrumentation.trace.annotations.TracedMethod <br>
 * - io.opentelemetry.spring.instrumentation.trace.annotations.TracedClass
 */
@Configuration
@EnableConfigurationProperties(TraceAspectProperties.class)
@ConditionalOnProperty(prefix = "opentelemetry.trace.aspects", name = "enabled")
public class TraceAspectAutoConfiguration {

  @Autowired private Tracer tracer;

  @Bean
  public TraceClassAspect traceClassAspect() {
    return new TraceClassAspect(tracer);
  }

  @Bean
  public TraceMethodAspect traceMethodAspect() {
    return new TraceMethodAspect(tracer);
  }
}
