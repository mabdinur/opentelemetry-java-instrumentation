package io.otel.instrumentation.spring.tools.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Traced specifies the annotated method should be included in the Trace.
 *
 * @since 0.5.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TracedMethod {

  /**
   * The optional custom span name.
   *
   * @return the optional custom span name; if not specified the method name will be used as the
   *     span name
   */
  String name() default "";
  boolean isEvent() default false;
}