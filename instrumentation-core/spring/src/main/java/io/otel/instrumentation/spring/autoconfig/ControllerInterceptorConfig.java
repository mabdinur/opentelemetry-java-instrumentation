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
package io.otel.instrumentation.spring.autoconfig;

import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.TracingContextUtils;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(prefix = "opentelemetry.autoconfig", name = "controllerTraceEnabled",
    matchIfMissing = true)
public class ControllerInterceptorConfig implements WebMvcConfigurer {

  @Autowired
  Tracer tracer;

  class ControllerInterceptor implements HandlerInterceptor {
    private final Logger LOG = Logger.getLogger(ControllerInterceptor.class.getName());

    private HttpTextFormat.Getter<HttpServletRequest> getter =
        new HttpTextFormat.Getter<HttpServletRequest>() {
          public String get(HttpServletRequest req, String key) {
            return req.getHeader(key);
          }
        };

    private HttpTextFormat.Setter<HttpServletResponse> setter =
        new HttpTextFormat.Setter<HttpServletResponse>() {
          @Override
          public void set(HttpServletResponse response, String key, String value) {
            response.addHeader(key, value);
          }
        };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
        Object handler) throws Exception {

      Context context = OpenTelemetry.getPropagators().getHttpTextFormat()
          .extract(Context.current(), request, getter);

      Span span = createSpanWithParent(request, context);
      span.setAttribute("handler", "pre");
      tracer.withSpan(span);
      
      LOG.info("ControllerInterceptor prehandle called");

      return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
        ModelAndView modelAndView) throws Exception {

      Span currentSpan = tracer.getCurrentSpan();
      currentSpan.setAttribute("handler", "post");
      OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), response,
          setter);
      currentSpan.end();
      
      LOG.info("ControllerInterceptor posthandler called");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
        Object handler, Exception exception) throws Exception {}

    private Span createSpanWithParent(HttpServletRequest request, Context context) {
      Span parentSpan = TracingContextUtils.getSpan(context);

      if (parentSpan.getContext().isValid()) {
        return tracer.spanBuilder(request.getRequestURI()).setParent(parentSpan).startSpan();
      }

      Span span = tracer.spanBuilder(request.getRequestURI()).startSpan();
      span.addEvent("Parent Span Not Found");

      return span;
    }
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new ControllerInterceptor());
  }
}
