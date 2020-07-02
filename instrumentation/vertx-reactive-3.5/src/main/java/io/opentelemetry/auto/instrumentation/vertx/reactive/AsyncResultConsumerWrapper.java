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

package io.opentelemetry.auto.instrumentation.vertx.reactive;

import io.grpc.Context;
import io.opentelemetry.context.ContextUtils;
import io.opentelemetry.context.Scope;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AsyncResultConsumerWrapper implements Consumer<Handler<AsyncResult<?>>> {
  private final Consumer<Handler<AsyncResult<?>>> delegate;
  private final Context executionContext;

  public AsyncResultConsumerWrapper(
      final Consumer<Handler<AsyncResult<?>>> delegate, Context executionContext) {
    this.delegate = delegate;
    this.executionContext = executionContext;
  }

  @Override
  public void accept(final Handler<AsyncResult<?>> asyncResultHandler) {
    if (executionContext != null) {
      try (final Scope scope = ContextUtils.withScopedContext(executionContext)) {
        delegate.accept(asyncResultHandler);
      }
    } else {
      delegate.accept(asyncResultHandler);
    }
  }

  public static Consumer<Handler<AsyncResult<?>>> wrapIfNeeded(
      final Consumer<Handler<AsyncResult<?>>> delegate, final Context executionContext) {
    if (!(delegate instanceof AsyncResultConsumerWrapper)) {
      log.debug("Wrapping consumer {}", delegate);
      return new AsyncResultConsumerWrapper(delegate, executionContext);
    }
    return delegate;
  }
}
