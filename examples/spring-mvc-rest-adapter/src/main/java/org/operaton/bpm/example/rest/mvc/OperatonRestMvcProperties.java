/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.example.rest.mvc;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Spring MVC REST adapter.
 *
 * <p>The {@code basePath} plays the same role for the adapter that
 * {@code spring.jersey.application-path} plays for the Jersey stack: it is the
 * single prefix every adapter endpoint is served under. It defaults to
 * {@code /engine-rest} so the adapter is wire-compatible with the JAX-RS API
 * out of the box.
 */
@ConfigurationProperties("operaton.rest-mvc")
public class OperatonRestMvcProperties {

  /** Base path all adapter controllers are served under. */
  private String basePath = "/engine-rest";

  /**
   * Name of the process engine to serve. {@code null} selects the default
   * engine, which mirrors the JAX-RS {@code /engine-rest/...} (as opposed to
   * {@code /engine-rest/engine/{name}/...}) behaviour.
   */
  private String engineName;

  public String getBasePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public String getEngineName() {
    return engineName;
  }

  public void setEngineName(String engineName) {
    this.engineName = engineName;
  }
}
