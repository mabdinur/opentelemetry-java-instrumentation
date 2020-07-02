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
import listener.Config
import org.hornetq.jms.client.HornetQMessageConsumer
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.listener.adapter.MessagingMessageListenerAdapter

import javax.jms.ConnectionFactory

import static JMS2Test.consumerSpan
import static JMS2Test.producerSpan

class SpringListenerJMS2Test extends AgentTestRunner {

  def "receiving message in spring listener generates spans"() {
    setup:
    def context = new AnnotationConfigApplicationContext(Config)
    def factory = context.getBean(ConnectionFactory)
    def template = new JmsTemplate(factory)
    template.convertAndSend("SpringListenerJMS2", "a message")

    expect:
    assertTraces(2) {
      trace(0, 2) {
        producerSpan(it, 0, "queue/SpringListenerJMS2")
        consumerSpan(it, 1, "queue/SpringListenerJMS2", true, MessagingMessageListenerAdapter, span(0))
      }
      trace(1, 1) {
        consumerSpan(it, 0, "queue/SpringListenerJMS2", false, HornetQMessageConsumer, traces[0][0])
      }
    }
  }
}
