package io.opentelemetry.instrumentation.spring.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

//"io.opentelemetry.instrumentation.spring", 
//basePackages = {"io.opentelemetry.instrumentation.spring"}
@Configuration
@EnableAspectJAutoProxy
@ComponentScan()
@EnableAutoConfiguration (exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
//@EnableWebMvc
class AppConfig {
}
