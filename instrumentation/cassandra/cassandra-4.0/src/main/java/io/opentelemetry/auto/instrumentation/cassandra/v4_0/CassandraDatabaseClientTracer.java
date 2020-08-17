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

package io.opentelemetry.auto.instrumentation.cassandra.v4_0;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.metadata.Node;
import io.opentelemetry.instrumentation.api.decorator.DatabaseClientTracer;
import io.opentelemetry.instrumentation.auto.api.jdbc.DbSystem;
import io.opentelemetry.trace.Span;
import java.net.InetSocketAddress;
import java.util.Optional;

public class CassandraDatabaseClientTracer extends DatabaseClientTracer<CqlSession, String> {
  public static final CassandraDatabaseClientTracer TRACER = new CassandraDatabaseClientTracer();

  @Override
  protected String getInstrumentationName() {
    return "io.opentelemetry.auto.cassandra-4.0";
  }

  @Override
  protected String normalizeQuery(String query) {
    return query;
  }

  @Override
  protected String dbSystem(final CqlSession session) {
    return DbSystem.CASSANDRA;
  }

  @Override
  protected String dbUser(final CqlSession session) {
    return null;
  }

  @Override
  protected String dbName(final CqlSession session) {
    return session.getKeyspace().map(CqlIdentifier::toString).orElse(null);
  }

  @Override
  protected InetSocketAddress peerAddress(CqlSession cqlSession) {
    return null;
  }

  public void onResponse(final Span span, final ExecutionInfo executionInfo) {
    Node coordinator = executionInfo.getCoordinator();
    if (coordinator != null) {
      Optional<InetSocketAddress> address = coordinator.getBroadcastRpcAddress();
      address.ifPresent(inetSocketAddress -> onPeerConnection(span, inetSocketAddress));
    }
  }
}
