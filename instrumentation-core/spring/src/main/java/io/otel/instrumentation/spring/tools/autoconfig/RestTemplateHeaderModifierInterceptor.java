package io.otel.instrumentation.spring.tools.autoconfig;

import java.io.IOException;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

@Component
public class RestTemplateHeaderModifierInterceptor implements ClientHttpRequestInterceptor {

  private static final Logger LOG =
      Logger.getLogger(RestTemplateHeaderModifierInterceptor.class.getName());

  @Autowired
  private Tracer tracer;

  private static HttpTextFormat.Setter<HttpRequest> setter =
      new HttpTextFormat.Setter<HttpRequest>() {
        @Override
        public void set(HttpRequest carrier, String key, String value) {
          carrier.getHeaders().set(key, value);
        }
      };

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {

    Span currentSpan = tracer.getCurrentSpan();
    currentSpan.addEvent("External request sent");

    OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), request, setter);

    ClientHttpResponse response = execution.execute(request, body);

    LOG.info(String.format("Response: %s", response.toString()));

    return response;
  }
}

