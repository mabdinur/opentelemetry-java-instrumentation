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
package io.otel.instrumentation.spring.tracer;

import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.exporters.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.trace.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@ConfigurationProperties(prefix = "opentelemetry.tracer")
public class OtelConfig {

  private String tracerName = "otel-spring-tracer";

  private boolean loggingExpoerterIsEnabled = false;

  @Bean
  public Tracer otelTracer() throws Exception {
    final Tracer tracer = OpenTelemetry.getTracer(tracerName);

    setLoggingExporter();

    return tracer;
  }

  private void setLoggingExporter() {
    if (loggingExpoerterIsEnabled) {
      SpanProcessor logProcessor =
          SimpleSpanProcessor.newBuilder(new LoggingSpanExporter()).build();
      OpenTelemetrySdk.getTracerProvider().addSpanProcessor(logProcessor);
    }
  }

  public String getTracername() {
    return tracerName;
  }

  public void setTracername(String tracerName) {
    this.tracerName = tracerName;
  }

  public boolean isLoggingExpoerterIsEnabled() {
    return loggingExpoerterIsEnabled;
  }

  public void setLoggingExpoerterIsEnabled(boolean loggingExpoerterIsEnabled) {
    this.loggingExpoerterIsEnabled = loggingExpoerterIsEnabled;
  }
}
