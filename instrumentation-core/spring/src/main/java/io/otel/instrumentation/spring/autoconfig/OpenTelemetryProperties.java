package io.otel.instrumentation.spring.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opentelemetry.autoconfig")
public class OpenTelemetryProperties {

  private boolean controllerTraceEnabled = true;

  private boolean restTemplateTraceEnabled = true;

  private boolean apacheHttpTraceEnabled = true;

  private boolean webClientTraceEnabled = true;

  private boolean grpcClientTraceEnabled = true;

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
