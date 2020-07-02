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

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.Response
import java.util.concurrent.Executors

import static io.opentelemetry.auto.test.base.HttpServerTest.ServerEndpoint.ERROR
import static io.opentelemetry.auto.test.base.HttpServerTest.ServerEndpoint.EXCEPTION
import static io.opentelemetry.auto.test.base.HttpServerTest.ServerEndpoint.PATH_PARAM
import static io.opentelemetry.auto.test.base.HttpServerTest.ServerEndpoint.QUERY_PARAM
import static io.opentelemetry.auto.test.base.HttpServerTest.ServerEndpoint.REDIRECT
import static io.opentelemetry.auto.test.base.HttpServerTest.ServerEndpoint.SUCCESS

class DropwizardAsyncTest extends DropwizardTest {

  Class testApp() {
    AsyncTestApp
  }

  Class testResource() {
    AsyncServiceResource
  }

  static class AsyncTestApp extends Application<Configuration> {
    @Override
    void initialize(Bootstrap<Configuration> bootstrap) {
    }

    @Override
    void run(Configuration configuration, Environment environment) {
      environment.jersey().register(AsyncServiceResource)
    }
  }

  @Path("/")
  static class AsyncServiceResource {
    final executor = Executors.newSingleThreadExecutor()

    @GET
    @Path("success")
    void success(@Suspended final AsyncResponse asyncResponse) {
      executor.execute {
        controller(SUCCESS) {
          asyncResponse.resume(Response.status(SUCCESS.status).entity(SUCCESS.body).build())
        }
      }
    }

    @GET
    @Path("query")
    Response query_param(@QueryParam("some") String param, @Suspended final AsyncResponse asyncResponse) {
      executor.execute {
        controller(QUERY_PARAM) {
          asyncResponse.resume(Response.status(QUERY_PARAM.status).entity("some=$param".toString()).build())
        }
      }
    }

    @GET
    @Path("redirect")
    void redirect(@Suspended final AsyncResponse asyncResponse) {
      executor.execute {
        controller(REDIRECT) {
          asyncResponse.resume(Response.status(REDIRECT.status).location(new URI(REDIRECT.body)).build())
        }
      }
    }

    @GET
    @Path("error-status")
    void error(@Suspended final AsyncResponse asyncResponse) {
      executor.execute {
        controller(ERROR) {
          asyncResponse.resume(Response.status(ERROR.status).entity(ERROR.body).build())
        }
      }
    }

    @GET
    @Path("exception")
    void exception(@Suspended final AsyncResponse asyncResponse) {
      executor.execute {
        controller(EXCEPTION) {
          def ex = new Exception(EXCEPTION.body)
          asyncResponse.resume(ex)
          throw ex
        }
      }
    }

    @GET
    @Path("path/{id}/param")
    Response path_param(@PathParam("id") int param, @Suspended final AsyncResponse asyncResponse) {
      executor.execute {
        controller(PATH_PARAM) {
          asyncResponse.resume(Response.status(PATH_PARAM.status).entity(param.toString()).build())
        }
      }
    }
  }
}
