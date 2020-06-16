package io.opentelemetry.instrumentation.spring.telemetry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.exporters.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.trace.Tracer;

//@Configuration
public class OtelConfig {

  private static final String TRACER_NAME = "open-telemetry-tracer";

  @Bean
  public Tracer otelTracer() throws Exception {
    final Tracer tracer = OpenTelemetry.getTracer(TRACER_NAME);
    SpanProcessor logProcessor = SimpleSpanProcessor.newBuilder(new LoggingSpanExporter()).build();
    OpenTelemetrySdk.getTracerProvider().addSpanProcessor(logProcessor);
    return tracer;
  }
}