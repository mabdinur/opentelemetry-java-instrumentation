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

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

import static io.opentelemetry.auto.test.utils.TraceUtils.basicSpan
import static io.opentelemetry.auto.test.utils.TraceUtils.runUnderTrace

class FilterTest extends AgentTestRunner {
  static {
    System.setProperty("ota.integration.servlet-filter.enabled", "true")
  }

  def "test doFilter no-parent"() {
    when:
    filter.doFilter(null, null, null)

    then:
    assertTraces(0) {}

    where:
    filter = new TestFilter()
  }

  def "test doFilter with parent"() {
    when:
    runUnderTrace("parent") {
      filter.doFilter(null, null, null)
    }

    then:
    assertTraces(1) {
      trace(0, 2) {
        basicSpan(it, 0, "parent")
        span(1) {
          operationName "${filter.class.simpleName}.doFilter"
          childOf span(0)
          tags {
          }
        }
      }
    }

    where:
    filter << [new TestFilter(), new TestFilter() {}]
  }

  def "test doFilter exception"() {
    setup:
    def ex = new Exception("some error")
    def filter = new TestFilter() {
      @Override
      void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        throw ex
      }
    }

    when:
    runUnderTrace("parent") {
      filter.doFilter(null, null, null)
    }

    then:
    def th = thrown(Exception)
    th == ex

    assertTraces(1) {
      trace(0, 2) {
        basicSpan(it, 0, "parent", null, ex)
        span(1) {
          operationName "${filter.class.simpleName}.doFilter"
          childOf span(0)
          errored true
          tags {
            errorTags(ex.class, ex.message)
          }
        }
      }
    }
  }

  static class TestFilter implements Filter {

    @Override
    void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    }

    @Override
    void destroy() {
    }
  }
}
