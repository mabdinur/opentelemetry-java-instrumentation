package io.opentelemetry.instrumentation.spring.autoconfig;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

//@Configuration
public class RestClientConfig {

  @Autowired
  RestTemplateHeaderModifierInterceptor restTemplateHeaderModifierInterceptor;
  
  @Autowired(required=false)
  RestTemplate restTemplate;

  @Bean
  public RestTemplate restTemplate(RestTemplate restTemplate) {
    
    if(restTemplate == null) {
      restTemplate = new RestTemplate();
    }

    List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
    if (interceptors.isEmpty()) {
      interceptors = new ArrayList<>();
    }
    interceptors.add(restTemplateHeaderModifierInterceptor);
    restTemplate.setInterceptors(interceptors);

    return restTemplate;
  }
}

