package io.otel.instrumentation.spring.tools.autoconfig;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

  @Autowired
  RestTemplateHeaderModifierInterceptor restTemplateHeaderModifierInterceptor;
  
  RestTemplate restTemplate;
  
  @Autowired(required=false)
  public RestClientConfig(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
  
  public RestClientConfig() {
    restTemplate = new RestTemplate();
  }

  @Bean
  public RestTemplate restTemplate() {
    
    List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
    if (interceptors.isEmpty()) {
      interceptors = new ArrayList<>();
    }
    interceptors.add(restTemplateHeaderModifierInterceptor);
    restTemplate.setInterceptors(interceptors);

    return restTemplate;
  }
}

