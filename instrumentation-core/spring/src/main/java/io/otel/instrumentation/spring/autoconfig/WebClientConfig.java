package io.otel.instrumentation.spring.autoconfig;

import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

@Component
public class WebClientConfig {

  @Autowired
  Tracer tracer;

  private final static Logger LOG = Logger.getLogger(WebClientConfig.class.getName());

  private static HttpTextFormat.Setter<ClientRequest> setter =
      new HttpTextFormat.Setter<ClientRequest>() {
        @Override
        public void set(ClientRequest carrier, String key, String value) {
          carrier.headers().add(key, value);
        }
      };

  public ExchangeFilterFunction otelAddTraceFilter() {
    return (clientRequest, next) -> {
      Span currentSpan = tracer.getCurrentSpan();
      currentSpan.addEvent("External request sent");
      OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), clientRequest,
          setter);

      LOG.info(String.format("Request sent from ClientHttpRequestInterceptor"));

      return next.exchange(clientRequest);
    };
  }
  
  public ExchangeFilterFunction otelAddTraceFilter(Tracer tracer) {
    return (clientRequest, next) -> {
      Span currentSpan = tracer.getCurrentSpan();
      currentSpan.addEvent("External request sent");
      OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), clientRequest,
          setter);

      LOG.info(String.format("Request sent using otelAddTraceFilter"));

      return next.exchange(clientRequest);
    };
  }

}
