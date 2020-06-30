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
package io.opentelemetry.spring.instrumentation.trace.httpclients;

import io.opentelemetry.trace.Tracer;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * BeanProcessor Implementation inspired by: <br>
 *
 * @see <a href=
 *     "https://github.com/spring-cloud/spring-cloud-sleuth/blob/master/spring-cloud-sleuth-core/src/main/java/org/springframework/cloud/sleuth/instrument/web/client/TraceWebClientAutoConfiguration.java">
 *     spring-cloud-sleuth-core </a>
 */
public final class ApacheHttpClientBeanPostProcessor implements BeanPostProcessor {

  private final Tracer tracer;

  public ApacheHttpClientBeanPostProcessor(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (bean instanceof HttpClientBuilder) {
      HttpClientBuilder httpClientBuilder = (HttpClientBuilder) bean;
      httpClientBuilder.addInterceptorFirst(new ApacheHttpClientInterceptor(tracer));
      return httpClientBuilder;
    }
    return bean;
  }
}
