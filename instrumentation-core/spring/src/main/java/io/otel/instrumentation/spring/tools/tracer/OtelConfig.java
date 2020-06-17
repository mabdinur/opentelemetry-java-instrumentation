package io.otel.instrumentation.spring.tools.tracer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.exporters.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.trace.Tracer;

@Configuration
@PropertySource(value = "classpath:application.properties")
public class OtelConfig {
  
  @Value("${opentelemetry.instrumentation.tracer.name:otel-trace}")
  private String tracerName;
  
  @Value("${opentelemetry.instrumentation.loggingExpoerter.isEnabled:true}")
  public boolean loggingExpoerterIsEnabled;
  
  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
      return new PropertySourcesPlaceholderConfigurer();
  }
  
  
  @Bean
  public Tracer otelTracer() throws Exception {
    final Tracer tracer = OpenTelemetry.getTracer(tracerName);

    setLoggingExporter();
      
    return tracer;
  }

  private void setLoggingExporter() {
    if(loggingExpoerterIsEnabled) {
      SpanProcessor logProcessor = SimpleSpanProcessor.newBuilder(new LoggingSpanExporter()).build();
      OpenTelemetrySdk.getTracerProvider().addSpanProcessor(logProcessor);
    }
  }
}