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

package io.opentelemetry.instrumentation.spring.autoconfigure.httpclients.resttemplate;

import io.opentelemetry.instrumentation.spring.autoconfigure.httpclients.HttpClientsProperties;
import io.opentelemetry.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configures {@link RestTemplate} for tracing.
 *
 * <p>Adds Open Telemetry instrumentation to RestTemplate beans after initialization
 */
@Configuration
@ConditionalOnClass(RestTemplate.class)
@EnableConfigurationProperties(HttpClientsProperties.class)
@ConditionalOnProperty(
    prefix = "opentelemetry.trace.httpclients",
    name = "enabled",
    matchIfMissing = true)
public class RestTemplateAutoConfiguration {

  @Bean
  @Autowired
  public RestTemplateBeanPostProcessor otelRestTemplateBeanPostProcessor(final Tracer tracer) {
    return new RestTemplateBeanPostProcessor(tracer);
  }
}
