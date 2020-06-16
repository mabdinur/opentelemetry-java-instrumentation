package io.opentelemetry.instrumentation.spring.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
//import io.opentelemetry.instrumentation.spring.config.AopConfig;

@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
public class K {
  
  public static void main(String[] args) throws Exception {
    SpringApplication.run(K.class, args);
  }

}
