package io.opentelemetry.instrumentation.spring.autoconfig;

import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.TracingContextUtils;

//@Component
public class TraceInterceptor implements HandlerInterceptor {

  private static final Logger LOG = Logger.getLogger(TraceInterceptor.class.getName());

  private static HttpTextFormat.Getter<HttpServletRequest> getter =
      new HttpTextFormat.Getter<HttpServletRequest>() {
        public String get(HttpServletRequest req, String key) {
          return req.getHeader(key);
        }
      };

  private static HttpTextFormat.Setter<HttpServletResponse> setter =
      new HttpTextFormat.Setter<HttpServletResponse>() {
        @Override
        public void set(HttpServletResponse response, String key, String value) {
          response.addHeader(key, value);
        }
      };

  @Autowired
  private Tracer tracer;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    Span span;
    try {

      Context context = OpenTelemetry.getPropagators().getHttpTextFormat()
          .extract(Context.current(), request, getter);

      span = tracer.spanBuilder(request.getRequestURI())
          .setParent(TracingContextUtils.getSpan(context)).startSpan();
      span.setAttribute("handler", "pre");
    } catch (Exception e) {
      span = tracer.spanBuilder(request.getRequestURI()).startSpan();
      span.setAttribute("handler", "pre");

      span.addEvent(e.toString());
      span.setAttribute("warn", true);
    }
    tracer.withSpan(span);

    LOG.info("Pre Handle Called");
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) throws Exception {

    Span currentSpan = tracer.getCurrentSpan();
    currentSpan.setAttribute("handler", "post");
    OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), response, setter);
    currentSpan.end();
    LOG.info("Post Handler Called");
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception exception) throws Exception {}
}

