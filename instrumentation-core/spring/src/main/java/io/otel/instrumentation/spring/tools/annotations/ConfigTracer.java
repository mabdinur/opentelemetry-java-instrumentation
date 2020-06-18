package io.otel.instrumentation.spring.tools.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Wraps {@literal @}Configuration and {@literal @}ComponentScan(basePackages =
 * "io.otel.instrumentation.spring.tools")
 * 
 * Auto-configures OpenTelemetry instrumentation tools and interceptors
 * 
 * 
 * Put this annotation under the proje
 *
 * @since 0.5.0
 */

@Configuration
@ComponentScan(basePackages = "io.otel.instrumentation.spring.tools")
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigTracer {
  /**
   * The optional custom span name.
   *
   * @return the optional custom span name; if not specified the method name will be used as the
   *         span name
   */
  String name() default "";

  boolean includingLoggingExporter() default true;
}
