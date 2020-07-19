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

package io.opentelemetry.instrumentation.spring.autoconfigure;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for OpenTelemetry Tracer
 *
 * <p>Sets default tracer name and sampler probability
 */
@ConfigurationProperties(prefix = "opentelemetry.trace.tracer")
public final class TracerProperties {
  // TODO: Add support for Span Batch Processor

  private String name = "otel-spring-tracer";

  @DecimalMin("0.0")
  @DecimalMax("1.0")
  private double samplerProbability = 1.0;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getSamplerProbability() {
    return samplerProbability;
  }

  public void setSamplerProbability(double samplerProbability) {
    this.samplerProbability = samplerProbability;
  }
}
