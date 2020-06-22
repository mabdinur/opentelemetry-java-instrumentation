/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
// @PropertySource("classpath:application.properties")
public class InterceptorConfig implements WebMvcConfigurer {

  @Autowired Tracer tracer;

  class TraceInterceptor implements HandlerInterceptor {
    private final Logger LOG = Logger.getLogger(TraceInterceptor.class.getName());

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
    public boolean preHandle(
        HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
      Span span;
      try {

        Context context =
            OpenTelemetry.getPropagators()
                .getHttpTextFormat()
                .extract(Context.current(), request, getter);

        span =
            tracer
                .spanBuilder(request.getRequestURI())
                .setParent(TracingContextUtils.getSpan(context))
                .startSpan();
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
    public void postHandle(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        ModelAndView modelAndView)
        throws Exception {

      Span currentSpan = tracer.getCurrentSpan();
      currentSpan.setAttribute("handler", "post");
      OpenTelemetry.getPropagators()
          .getHttpTextFormat()
          .inject(Context.current(), response, setter);
      currentSpan.end();
      LOG.info("Post Handler Called");
    }

    @Override
    public void afterCompletion(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        Exception exception)
        throws Exception {}
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new TraceInterceptor());
  }
}
