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

package io.opentelemetry.auto.instrumentation.reactor;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import io.opentelemetry.auto.tooling.Instrumenter;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public final class ReactorHooksInstrumentation extends Instrumenter.Default {

  public ReactorHooksInstrumentation() {
    super("reactor-core");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("reactor.core.publisher.Hooks");
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "io.opentelemetry.instrumentation.reactor.TracingPublishers",
      "io.opentelemetry.instrumentation.reactor.TracingPublishers$MonoTracingPublisher",
      "io.opentelemetry.instrumentation.reactor.TracingPublishers$ParallelFluxTracingPublisher",
      "io.opentelemetry.instrumentation.reactor.TracingPublishers$ConnectableFluxTracingPublisher",
      "io.opentelemetry.instrumentation.reactor.TracingPublishers$GroupedFluxTracingPublisher",
      "io.opentelemetry.instrumentation.reactor.TracingPublishers$FluxTracingPublisher",
      "io.opentelemetry.instrumentation.reactor.TracingPublishers$FuseableMonoTracingPublisher",
      "io.opentelemetry.instrumentation.reactor.TracingPublishers$FuseableParallelFluxTracingPublisher",
      "io.opentelemetry.instrumentation.reactor.TracingPublishers$FuseableConnectableFluxTracingPublisher",
      "io.opentelemetry.instrumentation.reactor.TracingPublishers$FuseableGroupedFluxTracingPublisher",
      "io.opentelemetry.instrumentation.reactor.TracingPublishers$FuseableFluxTracingPublisher",
      "io.opentelemetry.instrumentation.reactor.TracingSubscriber"
    };
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isTypeInitializer().or(named("resetOnEachOperator")), packageName + ".ReactorHooksAdvice");
  }
}
