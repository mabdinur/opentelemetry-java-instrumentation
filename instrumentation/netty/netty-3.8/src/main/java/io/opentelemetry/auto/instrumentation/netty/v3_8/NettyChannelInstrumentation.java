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

package io.opentelemetry.auto.instrumentation.netty.v3_8;

import static io.opentelemetry.auto.tooling.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.auto.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import com.google.auto.service.AutoService;
import io.opentelemetry.auto.bootstrap.ContextStore;
import io.opentelemetry.auto.bootstrap.InstrumentationContext;
import io.opentelemetry.auto.instrumentation.netty.v3_8.server.NettyHttpServerDecorator;
import io.opentelemetry.auto.tooling.Instrumenter;
import io.opentelemetry.trace.Span;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.jboss.netty.channel.Channel;

@AutoService(Instrumenter.class)
public class NettyChannelInstrumentation extends Instrumenter.Default {
  public NettyChannelInstrumentation() {
    super(
        NettyChannelPipelineInstrumentation.INSTRUMENTATION_NAME,
        NettyChannelPipelineInstrumentation.ADDITIONAL_INSTRUMENTATION_NAMES);
  }

  @Override
  public ElementMatcher<ClassLoader> classLoaderMatcher() {
    // Optimization for expensive typeMatcher.
    return hasClassesNamed("org.jboss.netty.channel.Channel");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("org.jboss.netty.channel.Channel"));
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      packageName + ".AbstractNettyAdvice",
      packageName + ".ChannelTraceContext",
      packageName + ".ChannelTraceContext$Factory",
      packageName + ".server.NettyHttpServerDecorator",
    };
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    final Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        isMethod()
            .and(named("connect"))
            .and(returns(named("org.jboss.netty.channel.ChannelFuture"))),
        NettyChannelInstrumentation.class.getName() + "$ChannelConnectAdvice");
    return transformers;
  }

  @Override
  public Map<String, String> contextStore() {
    return Collections.singletonMap(
        "org.jboss.netty.channel.Channel", ChannelTraceContext.class.getName());
  }

  public static class ChannelConnectAdvice extends AbstractNettyAdvice {
    @Advice.OnMethodEnter
    public static void addConnectContinuation(@Advice.This final Channel channel) {
      final Span span = NettyHttpServerDecorator.TRACER.getCurrentSpan();
      if (span.getContext().isValid()) {
        final ContextStore<Channel, ChannelTraceContext> contextStore =
            InstrumentationContext.get(Channel.class, ChannelTraceContext.class);

        if (contextStore
                .putIfAbsent(channel, ChannelTraceContext.Factory.INSTANCE)
                .getConnectionContinuation()
            == null) {
          contextStore.get(channel).setConnectionContinuation(span);
        }
      }
    }
  }
}
