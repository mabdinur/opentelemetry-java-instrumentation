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

import example.GreeterGrpc
import example.Helloworld
import io.grpc.BindableService
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import io.opentelemetry.auto.instrumentation.api.MoreTags
import io.opentelemetry.auto.test.AgentTestRunner
import io.opentelemetry.auto.test.utils.PortUtils
import io.opentelemetry.trace.Status

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import static io.opentelemetry.trace.Span.Kind.CLIENT
import static io.opentelemetry.trace.Span.Kind.SERVER

class GrpcStreamingTest extends AgentTestRunner {

  def "test conversation #name"() {
    setup:
    def msgCount = serverMessageCount
    def serverReceived = new CopyOnWriteArrayList<>()
    def clientReceived = new CopyOnWriteArrayList<>()
    def error = new AtomicReference()

    BindableService greeter = new GreeterGrpc.GreeterImplBase() {
      @Override
      StreamObserver<Helloworld.Response> conversation(StreamObserver<Helloworld.Response> observer) {
        return new StreamObserver<Helloworld.Response>() {
          @Override
          void onNext(Helloworld.Response value) {

            serverReceived << value.message

            (1..msgCount).each {
              observer.onNext(value)
            }
          }

          @Override
          void onError(Throwable t) {
            error.set(t)
            observer.onError(t)
          }

          @Override
          void onCompleted() {
            observer.onCompleted()
          }
        }
      }
    }
    def port = PortUtils.randomOpenPort()
    Server server = ServerBuilder.forPort(port).addService(greeter).build().start()
    ManagedChannelBuilder channelBuilder = ManagedChannelBuilder.forAddress("localhost", port)

    // Depending on the version of gRPC usePlainText may or may not take an argument.
    try {
      channelBuilder.usePlaintext()
    } catch (MissingMethodException e) {
      channelBuilder.usePlaintext(true)
    }
    ManagedChannel channel = channelBuilder.build()
    GreeterGrpc.GreeterStub client = GreeterGrpc.newStub(channel).withWaitForReady()

    when:
    def observer = client.conversation(new StreamObserver<Helloworld.Response>() {
      @Override
      void onNext(Helloworld.Response value) {
        clientReceived << value.message
      }

      @Override
      void onError(Throwable t) {
        error.set(t)
      }

      @Override
      void onCompleted() {
      }
    })

    clientRange.each {
      def message = Helloworld.Response.newBuilder().setMessage("call $it").build()
      observer.onNext(message)
    }
    observer.onCompleted()

    then:
    error.get() == null
    TEST_WRITER.waitForTraces(1)
    error.get() == null
    serverReceived == clientRange.collect { "call $it" }
    clientReceived == serverRange.collect { clientRange.collect { "call $it" } }.flatten().sort()

    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          operationName "example.Greeter/Conversation"
          spanKind CLIENT
          parent()
          errored false
          status(Status.OK)
          tags {
            "$MoreTags.RPC_SERVICE" "Greeter"
            "$MoreTags.NET_PEER_NAME" "localhost"
            "$MoreTags.NET_PEER_PORT" port
          }
          (1..(clientMessageCount * serverMessageCount)).each {
            def messageId = it
            event(it - 1) {
              eventName "message"
              attributes {
                "message.type" "SENT"
                "message.id" messageId
              }
            }
          }
        }
        span(1) {
          operationName "example.Greeter/Conversation"
          spanKind SERVER
          childOf span(0)
          errored false
          status(Status.OK)
          tags {
            "$MoreTags.RPC_SERVICE" "Greeter"
            "$MoreTags.NET_PEER_IP" "127.0.0.1"
            "$MoreTags.NET_PEER_PORT" Long
          }
          clientRange.each {
            def messageId = it
            event(it - 1) {
              eventName "message"
              attributes {
                "message.type" "RECEIVED"
                "message.id" messageId
              }
            }
          }
        }
      }
    }

    cleanup:
    channel?.shutdownNow()?.awaitTermination(10, TimeUnit.SECONDS)
    server?.shutdownNow()?.awaitTermination()

    where:
    name | clientMessageCount | serverMessageCount
    "A"  | 1                  | 1
    "B"  | 2                  | 1
    "C"  | 1                  | 2
    "D"  | 2                  | 2
    "E"  | 3                  | 3

    clientRange = 1..clientMessageCount
    serverRange = 1..serverMessageCount
  }
}
