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

package io.opentelemetry.auto.instrumentation.spymemcached;

import io.opentelemetry.trace.Span;
import net.spy.memcached.MemcachedConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncCompletionListener extends CompletionListener<Void> {

  private static final Logger log = LoggerFactory.getLogger(SyncCompletionListener.class);

  public SyncCompletionListener(final MemcachedConnection connection, final String methodName) {
    super(connection, methodName);
  }

  @Override
  protected void processResult(final Span span, final Void future) {
    log.error("processResult was called on SyncCompletionListener. This should never happen. ");
  }

  public void done(final Throwable thrown) {
    closeSyncSpan(thrown);
  }
}
