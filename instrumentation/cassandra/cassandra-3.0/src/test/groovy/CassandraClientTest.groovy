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

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session
import io.opentelemetry.auto.instrumentation.api.MoreTags
import io.opentelemetry.auto.instrumentation.api.Tags
import io.opentelemetry.auto.test.AgentTestRunner
import io.opentelemetry.auto.test.asserts.TraceAssert
import io.opentelemetry.sdk.trace.data.SpanData
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import spock.lang.Shared

import static io.opentelemetry.auto.test.utils.TraceUtils.basicSpan
import static io.opentelemetry.auto.test.utils.TraceUtils.runUnderTrace
import static io.opentelemetry.trace.Span.Kind.CLIENT

class CassandraClientTest extends AgentTestRunner {

  @Shared
  Cluster cluster

  def setupSpec() {
    /*
     This timeout seems excessive but we've seen tests fail with timeout of 40s.
     TODO: if we continue to see failures we may want to consider using 'real' Cassandra
     started in container like we do for memcached. Note: this will complicate things because
     tests would have to assume they run under shared Cassandra and act accordingly.
      */
    EmbeddedCassandraServerHelper.startEmbeddedCassandra(EmbeddedCassandraServerHelper.CASSANDRA_RNDPORT_YML_FILE, 120000L)

    cluster = EmbeddedCassandraServerHelper.getCluster()

    /*
    Looks like sometimes our requests fail because Cassandra takes to long to respond,
    Increase this timeout as well to try to cope with this.
     */
    cluster.getConfiguration().getSocketOptions().setReadTimeoutMillis(120000)
  }

  def cleanupSpec() {
    EmbeddedCassandraServerHelper.cleanEmbeddedCassandra()
  }

  def "test sync"() {
    setup:
    Session session = cluster.connect(keyspace)

    session.execute(statement)

    expect:
    assertTraces(keyspace ? 2 : 1) {
      if (keyspace) {
        trace(0, 1) {
          cassandraSpan(it, 0, "USE $keyspace", null)
        }
      }
      trace(keyspace ? 1 : 0, 1) {
        cassandraSpan(it, 0, statement, keyspace)
      }
    }

    cleanup:
    session.close()

    where:
    statement                                                                                         | keyspace
    "DROP KEYSPACE IF EXISTS sync_test"                                                               | null
    "CREATE KEYSPACE sync_test WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':3}" | null
    "CREATE TABLE sync_test.users ( id UUID PRIMARY KEY, name text )"                                 | "sync_test"
    "INSERT INTO sync_test.users (id, name) values (uuid(), 'alice')"                                 | "sync_test"
    "SELECT * FROM users where name = 'alice' ALLOW FILTERING"                                        | "sync_test"
  }

  def "test async"() {
    setup:
    Session session = cluster.connect(keyspace)
    runUnderTrace("parent") {
      session.executeAsync(statement)
    }

    expect:
    assertTraces(keyspace ? 2 : 1) {
      if (keyspace) {
        trace(0, 1) {
          cassandraSpan(it, 0, "USE $keyspace", null)
        }
      }
      trace(keyspace ? 1 : 0, 2) {
        basicSpan(it, 0, "parent")
        cassandraSpan(it, 1, statement, keyspace, span(0))
      }
    }

    cleanup:
    session.close()

    where:
    statement                                                                                          | keyspace
    "DROP KEYSPACE IF EXISTS async_test"                                                               | null
    "CREATE KEYSPACE async_test WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':3}" | null
    "CREATE TABLE async_test.users ( id UUID PRIMARY KEY, name text )"                                 | "async_test"
    "INSERT INTO async_test.users (id, name) values (uuid(), 'alice')"                                 | "async_test"
    "SELECT * FROM users where name = 'alice' ALLOW FILTERING"                                         | "async_test"
  }

  def cassandraSpan(TraceAssert trace, int index, String statement, String keyspace, Object parentSpan = null, Throwable exception = null) {
    trace.span(index) {
      operationName statement
      spanKind CLIENT
      if (parentSpan == null) {
        parent()
      } else {
        childOf((SpanData) parentSpan)
      }
      tags {
        "$MoreTags.NET_PEER_NAME" "localhost"
        "$MoreTags.NET_PEER_IP" "127.0.0.1"
        "$MoreTags.NET_PEER_PORT" EmbeddedCassandraServerHelper.getNativeTransportPort()
        "$Tags.DB_TYPE" "cassandra"
        "$Tags.DB_INSTANCE" keyspace
        "$Tags.DB_STATEMENT" statement
      }
    }
  }

}
