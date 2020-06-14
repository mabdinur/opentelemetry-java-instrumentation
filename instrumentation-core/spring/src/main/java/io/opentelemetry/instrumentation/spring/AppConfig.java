package io.opentelemetry.instrumentation.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
@EnableAspectJAutoProxy
@Configuration
@ComponentScan
public class AppConfig {
  
}
