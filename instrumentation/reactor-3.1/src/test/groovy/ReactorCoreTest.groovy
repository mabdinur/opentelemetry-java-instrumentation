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

import io.opentelemetry.OpenTelemetry
import io.opentelemetry.auto.test.AgentTestRunner
import io.opentelemetry.auto.test.utils.TraceUtils
import io.opentelemetry.trace.DefaultSpan
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Shared

import java.time.Duration

import static io.opentelemetry.auto.test.utils.TraceUtils.basicSpan

class ReactorCoreTest extends AgentTestRunner {

  public static final String EXCEPTION_MESSAGE = "test exception"

  @Shared
  def addOne = { i ->
    addOneFunc(i)
  }

  @Shared
  def addTwo = { i ->
    addTwoFunc(i)
  }

  @Shared
  def throwException = {
    throw new RuntimeException(EXCEPTION_MESSAGE)
  }

  def "Publisher '#name' test"() {
    when:
    def result = runUnderTrace(publisherSupplier)

    then:
    result == expected
    and:
    assertTraces(1) {
      trace(0, workSpans + 2) {
        span(0) {
          operationName "trace-parent"
          parent()
          tags {
          }
        }
        span(1) {
          operationName "publisher-parent"
          childOf span(0)
          tags {
          }
        }

        basicSpan(it, 1, "publisher-parent", span(0))

        for (int i = 0; i < workSpans; i++) {
          span(i + 2) {
            operationName "add one"
            childOf span(1)
            tags {
            }
          }
        }
      }
    }

    where:
    name                  | expected | workSpans | publisherSupplier
    "basic mono"          | 2        | 1         | { -> Mono.just(1).map(addOne) }
    "two operations mono" | 4        | 2         | { -> Mono.just(2).map(addOne).map(addOne) }
    "delayed mono"        | 4        | 1         | { ->
      Mono.just(3).delayElement(Duration.ofMillis(100)).map(addOne)
    }
    "delayed twice mono"  | 6        | 2         | { ->
      Mono.just(4).delayElement(Duration.ofMillis(100)).map(addOne).delayElement(Duration.ofMillis(100)).map(addOne)
    }
    "basic flux"          | [6, 7]   | 2         | { -> Flux.fromIterable([5, 6]).map(addOne) }
    "two operations flux" | [8, 9]   | 4         | { ->
      Flux.fromIterable([6, 7]).map(addOne).map(addOne)
    }
    "delayed flux"        | [8, 9]   | 2         | { ->
      Flux.fromIterable([7, 8]).delayElements(Duration.ofMillis(100)).map(addOne)
    }
    "delayed twice flux"  | [10, 11] | 4         | { ->
      Flux.fromIterable([8, 9]).delayElements(Duration.ofMillis(100)).map(addOne).delayElements(Duration.ofMillis(100)).map(addOne)
    }

    "mono from callable"  | 12       | 2         | { ->
      Mono.fromCallable({ addOneFunc(10) }).map(addOne)
    }
  }

  def "Publisher error '#name' test"() {
    when:
    runUnderTrace(publisherSupplier)

    then:
    def exception = thrown RuntimeException
    exception.message == EXCEPTION_MESSAGE
    and:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          operationName "trace-parent"
          errored true
          parent()
          tags {
            errorTags(RuntimeException, EXCEPTION_MESSAGE)
          }
        }

        // It's important that we don't attach errors at the Reactor level so that we don't
        // impact the spans on reactor integrations such as netty and lettuce, as reactor is
        // more of a context propagation mechanism than something we would be tracking for
        // errors this is ok.
        basicSpan(it, 1, "publisher-parent", span(0))
      }
    }

    where:
    name   | publisherSupplier
    "mono" | { -> Mono.error(new RuntimeException(EXCEPTION_MESSAGE)) }
    "flux" | { -> Flux.error(new RuntimeException(EXCEPTION_MESSAGE)) }
  }

  def "Publisher step '#name' test"() {
    when:
    runUnderTrace(publisherSupplier)

    then:
    def exception = thrown RuntimeException
    exception.message == EXCEPTION_MESSAGE
    and:
    assertTraces(1) {
      trace(0, workSpans + 2) {
        span(0) {
          operationName "trace-parent"
          errored true
          parent()
          tags {
            errorTags(RuntimeException, EXCEPTION_MESSAGE)
          }
        }

        // It's important that we don't attach errors at the Reactor level so that we don't
        // impact the spans on reactor integrations such as netty and lettuce, as reactor is
        // more of a context propagation mechanism than something we would be tracking for
        // errors this is ok.
        basicSpan(it, 1, "publisher-parent", span(0))

        for (int i = 0; i < workSpans; i++) {
          span(i + 2) {
            operationName "add one"
            childOf span(1)
            tags {
            }
          }
        }
      }
    }

    where:
    name                 | workSpans | publisherSupplier
    "basic mono failure" | 1         | { -> Mono.just(1).map(addOne).map({ throwException() }) }
    "basic flux failure" | 1         | { ->
      Flux.fromIterable([5, 6]).map(addOne).map({ throwException() })
    }
  }

  def "Publisher '#name' cancel"() {
    when:
    cancelUnderTrace(publisherSupplier)

    then:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          operationName "trace-parent"
          parent()
          tags {
          }
        }

        basicSpan(it, 1, "publisher-parent", span(0))
      }
    }

    where:
    name         | publisherSupplier
    "basic mono" | { -> Mono.just(1) }
    "basic flux" | { -> Flux.fromIterable([5, 6]) }
  }

  def "Publisher chain spans have the correct parent for '#name'"() {
    when:
    runUnderTrace(publisherSupplier)

    then:
    assertTraces(1) {
      trace(0, workSpans + 2) {
        span(0) {
          operationName "trace-parent"
          parent()
          tags {
          }
        }

        basicSpan(it, 1, "publisher-parent", span(0))

        for (int i = 0; i < workSpans; i++) {
          span(i + 2) {
            operationName "add one"
            childOf span(1)
            tags {
            }
          }
        }
      }
    }

    where:
    name         | workSpans | publisherSupplier
    "basic mono" | 3         | { ->
      Mono.just(1).map(addOne).map(addOne).then(Mono.just(1).map(addOne))
    }
    "basic flux" | 5         | { ->
      Flux.fromIterable([5, 6]).map(addOne).map(addOne).then(Mono.just(1).map(addOne))
    }
  }

  def "Publisher chain spans have the correct parents from assembly time '#name'"() {
    when:
    runUnderTrace {
      // The "add one" operations in the publisher created here should be children of the publisher-parent
      Publisher<Integer> publisher = publisherSupplier()

      def tracer = OpenTelemetry.getTracerProvider().get("test")
      def intermediate = tracer.spanBuilder("intermediate").startSpan()
      // After this activation, the "add two" operations below should be children of this span
      def scope = tracer.withSpan(intermediate)
      try {
        if (publisher instanceof Mono) {
          return ((Mono) publisher).map(addTwo)
        } else if (publisher instanceof Flux) {
          return ((Flux) publisher).map(addTwo)
        }
        throw new IllegalStateException("Unknown publisher type")
      } finally {
        intermediate.end()
        scope.close()
      }
    }

    then:
    assertTraces(1) {
      trace(0, (workItems * 2) + 3) {
        span(0) {
          operationName "trace-parent"
          parent()
          tags {
          }
        }

        basicSpan(it, 1, "publisher-parent", span(0))
        basicSpan(it, 2, "intermediate", span(1))

        for (int i = 0; i < workItems; i++) {
          span(3 + i) {
            operationName "add two"
            childOf span(2)
            tags {
            }
          }
        }
        for (int i = 0; i < workItems; i++) {
          span(3 + workItems + i) {
            operationName "add one"
            childOf span(1)
            tags {
            }
          }
        }
      }
    }

    where:
    name         | workItems | publisherSupplier
    "basic mono" | 1         | { -> Mono.just(1).map(addOne) }
    "basic flux" | 2         | { -> Flux.fromIterable([1, 2]).map(addOne) }
  }

  def "Publisher chain spans can have the parent removed at assembly time '#name'"() {
    when:
    runUnderTrace {
      // The operations in the publisher created here all end up children of the publisher-parent
      Publisher<Integer> publisher = publisherSupplier()

      // After this activation, all additions to the assembly will create new traces
      def tracer = OpenTelemetry.getTracerProvider().get("test")
      def scope = tracer.withSpan(DefaultSpan.getInvalid())
      try {
        if (publisher instanceof Mono) {
          return ((Mono) publisher).map(addOne)
        } else if (publisher instanceof Flux) {
          return ((Flux) publisher).map(addOne)
        }
        throw new IllegalStateException("Unknown publisher type")
      } finally {
        scope.close()
      }
    }

    then:
    assertTraces(1 + workItems) {
      trace(0, 2 + workItems) {
        span(0) {
          operationName "trace-parent"
          parent()
          tags {
          }
        }

        basicSpan(it, 1, "publisher-parent", span(0))

        for (int i = 0; i < workItems; i++) {
          span(2 + i) {
            operationName "add one"
            childOf span(1)
            tags {
            }
          }
        }
      }
      for (int i = 0; i < workItems; i++) {
        trace(i + 1, 1) {
          span(0) {
            operationName "add one"
            parent()
            tags {
            }
          }
        }
      }
    }

    where:
    name         | workItems | publisherSupplier
    "basic mono" | 1         | { -> Mono.just(1).map(addOne) }
    "basic flux" | 2         | { -> Flux.fromIterable([1, 2]).map(addOne) }
  }

  def runUnderTrace(def publisherSupplier) {
    TraceUtils.runUnderTrace("trace-parent") {
      def tracer = OpenTelemetry.getTracerProvider().get("test")
      def span = tracer.spanBuilder("publisher-parent").startSpan()
      def scope = tracer.withSpan(span)
      try {
        def publisher = publisherSupplier()
        // Read all data from publisher
        if (publisher instanceof Mono) {
          return publisher.block()
        } else if (publisher instanceof Flux) {
          return publisher.toStream().toArray({ size -> new Integer[size] })
        }

        throw new RuntimeException("Unknown publisher: " + publisher)
      } finally {
        span.end()
        scope.close()
      }
    }
  }

  def cancelUnderTrace(def publisherSupplier) {
    TraceUtils.runUnderTrace("trace-parent") {
      def tracer = OpenTelemetry.getTracerProvider().get("test")
      def span = tracer.spanBuilder("publisher-parent").startSpan()
      def scope = tracer.withSpan(span)

      def publisher = publisherSupplier()
      publisher.subscribe(new Subscriber<Integer>() {
        void onSubscribe(Subscription subscription) {
          subscription.cancel()
        }

        void onNext(Integer t) {
        }

        void onError(Throwable error) {
        }

        void onComplete() {
        }
      })

      span.end()
      scope.close()
    }
  }

  static addOneFunc(int i) {
    TEST_TRACER.spanBuilder("add one").startSpan().end()
    return i + 1
  }

  static addTwoFunc(int i) {
    TEST_TRACER.spanBuilder("add two").startSpan().end()
    return i + 2
  }
}
