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

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
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
public class ControllerFilter implements Filter {
  
  @Autowired
  Tracer tracer;
  
  private final Logger LOG = Logger.getLogger(ControllerFilter.class.getName());

  private final HttpTextFormat.Getter<HttpServletRequest> GETTER =
      new HttpTextFormat.Getter<HttpServletRequest>() {
        public String get(HttpServletRequest req, String key) {
          return req.getHeader(key);
        }
      };

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    LOG.info("start doFilter");
    
    HttpServletRequest req = (HttpServletRequest) request;
    Context context = OpenTelemetry.getPropagators().getHttpTextFormat()
        .extract(Context.current(), req, GETTER);
    Span currentSpan = createSpanWithParent(req, context);
    try {
      tracer.withSpan(currentSpan);
      currentSpan.addEvent("handler afterCompletion");
      chain.doFilter(req, response);
    }finally {
      LOG.info("end doFilter");
      currentSpan.end();
    }
    
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
