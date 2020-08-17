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

import io.opentelemetry.auto.test.AgentTestRunner
import io.opentelemetry.auto.test.utils.ConfigUtils
import io.opentelemetry.test.annotation.TracedWithSpan
import io.opentelemetry.trace.Span

/**
 * This test verifies that auto instrumentation supports {@link io.opentelemetry.extensions.auto.annotations.WithSpan} contrib annotation.
 */
class WithSpanInstrumentationTest extends AgentTestRunner {

  static {
    ConfigUtils.updateConfig {
      System.setProperty("otel.trace.classes.exclude", WithSpanInstrumentationTest.name + "*")
      System.setProperty("otel.trace.annotated.methods.exclude", "${TracedWithSpan.name}[ignored]")
    }
  }

  def cleanupSpec() {
    ConfigUtils.updateConfig {
      System.clearProperty("otel.trace.classes.exclude")
      System.clearProperty("otel.trace.annotated.methods.exclude")
    }
  }

  def "should derive automatic name"() {
    setup:
    new TracedWithSpan().otel()

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          operationName "TracedWithSpan.otel"
          spanKind Span.Kind.INTERNAL
          parent()
          errored false
          attributes {
            "providerAttr" "Otel"
          }
        }
      }
    }
  }

  def "should take span name from annotation"() {
    setup:
    new TracedWithSpan().namedOtel()

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          operationName "manualName"
          parent()
          errored false
          attributes {
            "providerAttr" "Otel"
          }
        }
      }
    }
  }

  def "should take span kind from annotation"() {
    setup:
    new TracedWithSpan().oneOfAKind()

    expect:
    assertTraces(1) {
      trace(0, 1) {
        span(0) {
          operationName "TracedWithSpan.oneOfAKind"
          spanKind Span.Kind.PRODUCER
          parent()
          errored false
          attributes {
            "providerAttr" "Otel"
          }
        }
      }
    }
  }


  def "should ignore method excluded by trace.annotated.methods.exclude configuration"() {
    setup:
    new TracedWithSpan().ignored()

    expect:
    Thread.sleep(500) // sleep a bit just to make sure no span is captured
    assertTraces(0) {}
  }

}
