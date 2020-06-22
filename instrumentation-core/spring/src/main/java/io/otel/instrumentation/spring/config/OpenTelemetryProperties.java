package io.otel.instrumentation.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:opentelemetry.properties")
@ConfigurationProperties(prefix="opentelemetry.spring")
public class OpenTelemetryProperties {

  private String tracerName = "Otel Tracer";

  private boolean controllerTraceEnabled;

  private boolean restTemplateTraceEnabled;

  private boolean apacheHttpTraceEnabled;

  private boolean webClientTraceEnabled;

  private boolean grpcClientTraceEnabled;

  public String getTracerName() {
    return tracerName;
  }

  public void setTracerName(String tracerName) {
    this.tracerName = tracerName;
  }

  public boolean isControllerTraceEnabled() {
    return controllerTraceEnabled;
  }

  public void setControllerTraceEnabled(boolean controllerTraceEnabled) {
    this.controllerTraceEnabled = controllerTraceEnabled;
  }

  public boolean isRestTemplateTraceEnabled() {
    return restTemplateTraceEnabled;
  }

  public void setRestTemplateTraceEnabled(boolean restTemplateTraceEnabled) {
    this.restTemplateTraceEnabled = restTemplateTraceEnabled;
  }

  public boolean isApacheHttpTraceEnabled() {
    return apacheHttpTraceEnabled;
  }

  public void setApacheHttpTraceEnabled(boolean apacheHttpTraceEnabled) {
    this.apacheHttpTraceEnabled = apacheHttpTraceEnabled;
  }

  public boolean isWebClientTraceEnabled() {
    return webClientTraceEnabled;
  }

  public void setWebClientTraceEnabled(boolean webClientTraceEnabled) {
    this.webClientTraceEnabled = webClientTraceEnabled;
  }

  public boolean isGrpcClientTraceEnabled() {
    return grpcClientTraceEnabled;
  }

  public void setGrpcClientTraceEnabled(boolean grpcClientTraceEnabled) {
    this.grpcClientTraceEnabled = grpcClientTraceEnabled;
  }
}
