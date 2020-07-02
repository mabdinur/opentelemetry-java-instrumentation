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

import io.dropwizard.testing.junit.ResourceTestRule
import io.opentelemetry.auto.test.AgentTestRunner
import org.junit.ClassRule
import spock.lang.Shared

import static io.opentelemetry.auto.test.utils.TraceUtils.runUnderTrace
import static io.opentelemetry.trace.Span.Kind.INTERNAL

class JerseyTest extends AgentTestRunner {

  @Shared
  @ClassRule
  ResourceTestRule resources = ResourceTestRule.builder()
    .addResource(new Resource.Test1())
    .addResource(new Resource.Test2())
    .addResource(new Resource.Test3())
    .build()

  def "test #resource"() {
    when:
    // start a trace because the test doesn't go through any servlet or other instrumentation.
    def response = runUnderTrace("test.span") {
      resources.client().resource(resource).post(String)
    }

    then:
    response == expectedResponse

    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          operationName expectedSpanName
          tags {
          }
        }

        span(1) {
          childOf span(0)
          operationName controllerName
          tags {
          }
        }
      }
    }

    where:
    resource           | expectedSpanName           | controllerName | expectedResponse
    "/test/hello/bob"  | "POST /test/hello/{name}"  | "Test1.hello"  | "Test1 bob!"
    "/test2/hello/bob" | "POST /test2/hello/{name}" | "Test2.hello"  | "Test2 bob!"
    "/test3/hi/bob"    | "POST /test3/hi/{name}"    | "Test3.hello"  | "Test3 bob!"
  }

  def "test nested call"() {

    when:
    // start a trace because the test doesn't go through any servlet or other instrumentation.
    def response = runUnderTrace("test.span") {
      resources.client().resource(resource).post(String)
    }

    then:
    response == expectedResponse

    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          operationName expectedSpanName
          tags {
          }
        }
        span(1) {
          childOf span(0)
          operationName controller1Name
          spanKind INTERNAL
          tags {
          }
        }
      }
    }

    where:
    resource        | expectedSpanName     | controller1Name | expectedResponse
    "/test3/nested" | "POST /test3/nested" | "Test3.nested"  | "Test3 nested!"
  }
}
