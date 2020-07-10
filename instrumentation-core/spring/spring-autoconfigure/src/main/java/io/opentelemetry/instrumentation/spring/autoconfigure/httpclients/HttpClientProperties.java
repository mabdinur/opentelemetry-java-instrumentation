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
package io.opentelemetry.instrumentation.spring.autoconfigure.httpclients;

import org.springframework.boot.context.properties.ConfigurationProperties;
import io.opentelemetry.instrumentation.spring.autoconfigure.helpers.PropertiesBase;

/**
 * Loads opentelemetry.trace.httpclients.enabled from application.properties <br>
 * Sets default value to true if the configuration does not exist
 */
@ConfigurationProperties(prefix = "opentelemetry.trace.httpclients")
public final class HttpClientProperties extends PropertiesBase {}
