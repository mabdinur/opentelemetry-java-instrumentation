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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnClass(RestTemplate.class)
public class RestClientConfig {

  @Autowired private Tracer tracer;

  class RestTemplateHeaderModifierInterceptor implements ClientHttpRequestInterceptor {

    private final Logger LOG =
        Logger.getLogger(RestTemplateHeaderModifierInterceptor.class.getName());

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

      Span currentSpan = tracer.getCurrentSpan();
      currentSpan.addEvent("External request sent");

      OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), request, setter);

      ClientHttpResponse response = execution.execute(request, body);

      LOG.info(String.format("Request sent from ClientHttpRequestInterceptor"));

      return response;
    }
  }

  @Autowired
  @ConditionalOnBean(RestTemplate.class)
  public void restTemplate(RestTemplate restTemplate) {
    setInterceptor(restTemplate);
  }

  @Bean
  @ConditionalOnMissingBean(type = "org.springframework.web.client.RestTemplate")
  public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    setInterceptor(restTemplate);
    return restTemplate;
  }

  private void setInterceptor(RestTemplate restTemplate) {
    List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
    if (interceptors.isEmpty()) {
      interceptors = new ArrayList<>();
    }
    interceptors.add(new RestTemplateHeaderModifierInterceptor());
    restTemplate.setInterceptors(interceptors);
  }
}
