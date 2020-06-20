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
package io.otel.instrumentation.spring.annotations;

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
 * <p>Auto-configures OpenTelemetry instrumentation tools and interceptors
 *
 * <p>Put this annotation under the proje
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
   *     span name
   */
  String name() default "";

  boolean includingLoggingExporter() default true;
}
