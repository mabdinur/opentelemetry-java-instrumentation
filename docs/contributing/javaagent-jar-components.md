### Understanding the javaagent components

OpenTelemetry Auto Instrumentation java agent's jar can logically be divided
into 3 parts:

* Modules that live in the system class loader
* Modules that live in the bootstrap class loader
* Modules that live in the agent class loader

### Modules that live in the system class loader

#### `opentelemetry-javaagent` module

This module consists of single class
`io.opentelemetry.auto.bootstrap.AgentBootstrap` which implements [Java
instrumentation
agent](https://docs.oracle.com/javase/7/docs/api/java/lang/instrument/package-summary.html).
This class is loaded during application startup by application classloader.
Its sole responsibility is to push agent's classes into JVM's bootstrap
classloader and immediately delegate to
`io.opentelemetry.auto.bootstrap.Agent` (now in the bootstrap class loader)
class from there.

### Modules that live in the bootstrap class loader

#### `agent-bootstrap` module

`io.opentelemetry.auto.bootstrap.Agent` and a few other classes that live in the bootstrap class
loader but are not used directly by auto-instrumentation

#### `instrumentation-api` and `auto-api` modules

These modules contains support classes for actual instrumentations to be loaded
later and separately. These classes should be available from all possible
classloaders in the running application. For this reason `java-agent` puts
all these classes into JVM's bootstrap classloader. For the same reason this
module should be as small as possible and have as few dependencies as
possible. Otherwise, there is a risk of accidentally exposing this classes to
the actual application.

`instrumentation-api` contains classes that are needed for both library and auto-instrumentation,
while `auto-api` contains classes that are only needed for auto-instrumentation.

### Modules that live in the agent class loader

#### `agent-tooling` module and `instrumentation` submodules

Contains everything necessary to make instrumentation machinery work,
including integration with [ByteBuddy](https://bytebuddy.net/) and actual
library-specific instrumentations. As these classes depend on many classes
from different libraries, it is paramount to hide all these classes from the
host application. This is achieved in the following way:

- When `java-agent` module builds the final agent, it moves all classes from
`instrumentation` submodules and `agent-tooling` module into a separate
folder inside final jar file, called`inst`.
In addition, the extension of all class files is changed from `class` to `classdata`.
This ensures that general classloaders cannot find nor load these classes.
- When `io.opentelemetry.auto.bootstrap.Agent` starts up, it creates an
instance of `io.opentelemetry.instrumentation.auto.api.AgentClassLoader`, loads an
`io.opentelemetry.auto.tooling.AgentInstaller` from that `AgentClassLoader`
and then passes control on to the `AgentInstaller` (now in the
`AgentClassLoader`). The `AgentInstaller` then installs all of the
instrumentations with the help of ByteBuddy.

The complicated process above ensures that the majority of
auto-instrumentation agent's classes are totally isolated from application
classes, and an instrumented class from arbitrary classloader in JVM can
still access helper classes from bootstrap classloader.

#### Agent jar structure

If you now look inside
`opentelemetry-javaagent/build/libs/opentelemetry-javaagent-<version>-all.jar`, you will see the
following "clusters" of classes:

Available in the system class loader:

- `io/opentelemetry/auto/bootstrap/AgentBootstrap` - the one class from `opentelemetry-javaagent`
module

Available in the bootstrap class loader:

- `io/opentelemetry/auto/bootstrap/` - contains the `agent-bootstrap` module
- `io/opentelemetry/instrumentation/auto/api/` - contains the `auto-api` module
- `io/opentelemetry/auto/shaded/instrumentation/api/` - contains the `instrumentation-api` module,
 shaded during creation of `javaagent` jar file by Shadow Gradle plugin
- `io/opentelemetry/auto/shaded/io/` - contains the OpenTelemetry API and its dependency gRPC
Context, both shaded during creation of `javaagent` jar file by Shadow Gradle plugin
- `io/opentelemetry/auto/slf4j/` - contains SLF4J and its simple logger implementation, shaded
during creation of `javaagent` jar file by Shadow Gradle plugin

Available in the agent class loader:
- `inst/` - contains `agent-tooling` module and `instrumentation` submodules, loaded and isolated
inside `AgentClassLoader`. Including OpenTelemetry SDK (and the built-in exporters when using the
`-all` artifact).
