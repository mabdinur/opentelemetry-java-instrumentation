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

import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures @{Slf4jMdcAspect} */
@Configuration
@EnableConfigurationProperties(Slf4jAspectProperties.class)
@ConditionalOnProperty(
    prefix = "opentelemetry.trace.logging.sfl4j",
    name = "enabled",
    matchIfMissing = true)
@ConditionalOnClass({Aspect.class, MDC.class})
public class Slf4jAspectAutoConfiguration {

  @Bean
  public Slf4jMdcAspect slf4jAspect() {
    return new Slf4jMdcAspect();
  }
}
