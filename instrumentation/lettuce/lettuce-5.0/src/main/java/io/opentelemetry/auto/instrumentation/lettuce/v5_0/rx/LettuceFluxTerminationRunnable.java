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

package io.opentelemetry.auto.instrumentation.lettuce.v5_0.rx;

import static io.opentelemetry.auto.instrumentation.lettuce.v5_0.LettuceClientDecorator.DECORATE;
import static io.opentelemetry.auto.instrumentation.lettuce.v5_0.LettuceClientDecorator.TRACER;
import static io.opentelemetry.trace.Span.Kind.CLIENT;

import io.lettuce.core.protocol.RedisCommand;
import io.opentelemetry.auto.instrumentation.lettuce.v5_0.LettuceInstrumentationUtil;
import io.opentelemetry.trace.Span;
import java.util.function.Consumer;
import org.reactivestreams.Subscription;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Signal;
import reactor.core.publisher.SignalType;

public class LettuceFluxTerminationRunnable implements Consumer<Signal>, Runnable {

  private Span span = null;
  private int numResults = 0;
  private FluxOnSubscribeConsumer onSubscribeConsumer = null;

  public LettuceFluxTerminationRunnable(
      final RedisCommand command, final boolean finishSpanOnClose) {
    onSubscribeConsumer = new FluxOnSubscribeConsumer(this, command, finishSpanOnClose);
  }

  public FluxOnSubscribeConsumer getOnSubscribeConsumer() {
    return onSubscribeConsumer;
  }

  private void finishSpan(final boolean isCommandCancelled, final Throwable throwable) {
    if (span != null) {
      span.setAttribute("db.command.results.count", numResults);
      if (isCommandCancelled) {
        span.setAttribute("db.command.cancelled", true);
      }
      DECORATE.onError(span, throwable);
      DECORATE.beforeFinish(span);
      span.end();
    } else {
      LoggerFactory.getLogger(Flux.class)
          .error(
              "Failed to finish this.span, LettuceFluxTerminationRunnable cannot find this.span "
                  + "because it probably wasn't started.");
    }
  }

  @Override
  public void accept(final Signal signal) {
    if (SignalType.ON_COMPLETE.equals(signal.getType())
        || SignalType.ON_ERROR.equals(signal.getType())) {
      finishSpan(false, signal.getThrowable());
    } else if (SignalType.ON_NEXT.equals(signal.getType())) {
      ++numResults;
    }
  }

  @Override
  public void run() {
    if (span != null) {
      finishSpan(true, null);
    } else {
      LoggerFactory.getLogger(Flux.class)
          .error(
              "Failed to finish this.span to indicate cancellation, LettuceFluxTerminationRunnable"
                  + " cannot find this.span because it probably wasn't started.");
    }
  }

  public static class FluxOnSubscribeConsumer implements Consumer<Subscription> {

    private final LettuceFluxTerminationRunnable owner;
    private final RedisCommand command;
    private final boolean finishSpanOnClose;

    public FluxOnSubscribeConsumer(
        final LettuceFluxTerminationRunnable owner,
        final RedisCommand command,
        final boolean finishSpanOnClose) {
      this.owner = owner;
      this.command = command;
      this.finishSpanOnClose = finishSpanOnClose;
    }

    @Override
    public void accept(final Subscription subscription) {
      final Span span =
          TRACER
              .spanBuilder(LettuceInstrumentationUtil.getCommandName(command))
              .setSpanKind(CLIENT)
              .startSpan();
      owner.span = span;
      DECORATE.afterStart(span);
      if (finishSpanOnClose) {
        DECORATE.beforeFinish(span);
        span.end();
      }
    }
  }
}
