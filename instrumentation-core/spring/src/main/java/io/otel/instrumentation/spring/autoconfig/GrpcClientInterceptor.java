package io.otel.instrumentation.spring.autoconfig;

import java.util.logging.Logger;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Context;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

/**
 * Uses https://github.com/LogNet/grpc-spring-boot-starter
 * @author root
 *
 */
@GRpcGlobalInterceptor
public class GrpcClientInterceptor implements ClientInterceptor {

  @Autowired
  private Tracer tracer;

  private final Logger LOG = Logger.getLogger(GrpcClientInterceptor.class.getName());

  private static HttpTextFormat.Setter<Metadata> setter = new HttpTextFormat.Setter<Metadata>() {
    @Override
    public void set(final Metadata carrier, final String key, final String value) {
      carrier.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value);
    }
  };

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
      CallOptions callOptions, Channel next) {

    return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
        next.newCall(method, callOptions)) {
      @Override
      public void start(final Listener<RespT> responseListener, final Metadata headers) {
        Span currentSpan = tracer.getCurrentSpan();
        currentSpan.addEvent("External request sent");

        OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), headers,
            setter);

        LOG.info(String.format("Request sent from GRpcClientConfig"));

        super.start(responseListener, headers);
      }
    };
  }
}
