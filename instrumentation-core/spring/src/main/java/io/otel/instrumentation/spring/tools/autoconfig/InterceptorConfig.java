package io.otel.instrumentation.spring.tools.autoconfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
//@PropertySource("classpath:application.properties")
public class InterceptorConfig implements WebMvcConfigurer {
  @Autowired
  TraceInterceptor traceInterceptor;
  
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(traceInterceptor);
  }

}

