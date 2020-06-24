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
import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.TracingContextUtils;

@Configuration
@ConditionalOnProperty(prefix = "opentelemetry.autoconfig", name = "controllerTraceEnabled")
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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
        Object handler) throws Exception {

      Context context = OpenTelemetry.getPropagators().getHttpTextFormat()
          .extract(Context.current(), request, getter);

      Span span = createSpanWithParent(request, context);
      span.addEvent("handler pre");
      
      tracer.withSpan(span);

      LOG.info("ControllerInterceptor prehandle called");

      return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
        ModelAndView modelAndView) throws Exception {}

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
        Object handler, Exception exception) throws Exception {
      Span currentSpan = tracer.getCurrentSpan();
      currentSpan.addEvent("handler afterCompletion");
      currentSpan.end();

      LOG.info("ControllerInterceptor afterCompletion called");
    }

    private Span createSpanWithParent(HttpServletRequest request, Context context) {
      Span parentSpan = TracingContextUtils.getSpan(context);
      Span.Builder spanBuilder = tracer.spanBuilder(request.getRequestURI()).setSpanKind(Span.Kind.SERVER);
      
      if (parentSpan.getContext().isValid()) {
        return spanBuilder.setParent(parentSpan).startSpan();
      }
  
      Span span = spanBuilder.startSpan();
      span.addEvent("Parent Span Not Found");

      return span;
    }
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new ControllerInterceptor());
  }
}
