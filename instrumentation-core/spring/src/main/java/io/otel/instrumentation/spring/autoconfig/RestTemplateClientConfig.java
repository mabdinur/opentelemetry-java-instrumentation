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

import java.io.IOException;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;

@Configuration
@ConditionalOnBean(RestTemplate.class)
@ConditionalOnProperty(prefix="opentelemetry.autoconfig", name="restTemplateTraceEnabled")
public class RestTemplateClientConfig {
  
  @Autowired private Tracer tracer;
  
  class RestTemplateInterceptor implements ClientHttpRequestInterceptor {
    
    private final Logger LOG =
        Logger.getLogger(RestTemplateInterceptor.class.getName());

    private HttpTextFormat.Setter<HttpRequest> setter =
        new HttpTextFormat.Setter<HttpRequest>() {
          @Override
          public void set(HttpRequest carrier, String key, String value) {
            carrier.getHeaders().set(key, value);
          }
        };

    @Override
    public ClientHttpResponse intercept(
        HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
      
      String spanName = request.getMethodValue() +  " " + request.getURI().toString();
      Span currentSpan = tracer.spanBuilder(spanName).setSpanKind(Span.Kind.CLIENT).startSpan();
      
      try {
        OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), request, setter);
        ClientHttpResponse response = execution.execute(request, body);
        LOG.info(String.format("Request sent from RestTemplateInterceptor"));

        return response;
      }finally {
        currentSpan.end();
      }
    }
  }
  
  @Autowired
  public void restTemplate(RestTemplate restTemplate) {
    restTemplate.getInterceptors().add(new RestTemplateInterceptor());
  }

//  @Bean
//  @ConditionalOnMissingBean(type = "org.springframework.web.client.RestTemplate")
//  public RestTemplate restTemplate() {
//    RestTemplate restTemplate = newrestTemplate.getInterceptors().add(new RestTemplateInterceptor());
//    return restTemplate;
//  }
}
