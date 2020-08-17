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

package io.opentelemetry.auto.instrumentation.armeria.v1_0;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import com.linecorp.armeria.server.ServerBuilder;
import io.opentelemetry.auto.tooling.Instrumenter;
import io.opentelemetry.instrumentation.armeria.v1_0.server.OpenTelemetryService;
import java.util.Collections;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class ArmeriaServerInstrumentation extends AbstractArmeriaInstrumentation {
  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return named("com.linecorp.armeria.server.Server");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return Collections.singletonMap(
        isMethod().and(isPublic()).and(isStatic()).and(named("builder")),
        ArmeriaServerInstrumentation.class.getName() + "$AddDecoratorAdvice");
  }

  public static class AddDecoratorAdvice {
    @Advice.OnMethodExit
    public static void addDecorator(@Advice.Return ServerBuilder sb) {
      sb.decorator(OpenTelemetryService.newDecorator());
    }
  }
}
