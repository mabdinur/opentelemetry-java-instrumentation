package io.otel.instrumentation.spring.autoconfig;

import java.io.IOException;
import java.util.logging.Logger;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

@Configuration
@ConditionalOnBean(AbstractHttpClient.class)
@ConditionalOnProperty(prefix="opentelemetry.autoconfig", name="apacheHttpTraceEnabled")
public class ApacheHttpClientConfig {

  @Autowired private Tracer tracer;

  class ApacheClientInterceptor implements HttpRequestInterceptor {

    private final Logger LOG =
        Logger.getLogger(ApacheClientInterceptor.class.getName());

    private HttpTextFormat.Setter<HttpRequest> setter =
        new HttpTextFormat.Setter<HttpRequest>() {
          @Override
          public void set(HttpRequest carrier, String key, String value) {
            carrier.addHeader(key, value);
          }
        };

    @Override
    public void process(HttpRequest request, HttpContext context)
        throws HttpException, IOException {
      Span currentSpan = tracer.getCurrentSpan();
      currentSpan.addEvent("External request sent");
      OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), request, setter);
      LOG.info(String.format("Request sent from ApacheClientInterceptor")); 
    }
  }

  @Autowired
  public void addApacheClientInterceptor(AbstractHttpClient abstractHttpClient) {
    abstractHttpClient.addRequestInterceptor(new ApacheClientInterceptor());
  }
}
