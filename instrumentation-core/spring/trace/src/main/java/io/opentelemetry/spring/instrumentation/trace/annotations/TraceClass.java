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
package io.opentelemetry.spring.instrumentation.trace.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add {@literal}TraceClass to class definition to wrap public methods in a span
 *
 * @since 0.5.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceClass {

  /**
   * If {@code isEvent=false} the method call will be wrapped in a span <br>
   * If {@code isEvent=true} the method call will be logged as an event in the current span <br>
   * If {@code isEvent=true} and the parent span does not exist. A default span will be created and
   * the event will be logged
   */
  boolean isEvent() default false;
}
