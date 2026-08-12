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

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.rest.impl.OperatonRestResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guards the central claim of this module: the JAX-RS classes are all present,
 * and no JAX-RS runtime is.
 */
class NoJaxRsRuntimeTest {

  @Test
  void jaxRsResourceClassesAreStillOnTheClasspath() {
    // The whole point of the adapter is that these stay available to delegate to.
    assertThat(OperatonRestResources.getResourceClasses()).isNotEmpty();
    assertThat(OperatonRestResources.getConfigurationClasses()).isNotEmpty();
  }

  @Test
  void jaxRsAnnotationsAreStillPresentOnThoseClasses() {
    // Annotations are inert metadata without a runtime to scan them; they cost
    // nothing and are what keeps the classes usable in a JAX-RS deployment too.
    assertThat(org.operaton.bpm.engine.rest.ProcessDefinitionRestService.class
        .getAnnotation(jakarta.ws.rs.Produces.class)).isNotNull();
  }

  @Test
  void noJaxRsImplementationIsOnTheClasspath() {
    assertThatThrownBy(() -> Class.forName("org.glassfish.jersey.server.ResourceConfig"))
        .isInstanceOf(ClassNotFoundException.class);
    assertThatThrownBy(() -> Class.forName("org.jboss.resteasy.core.ResteasyDeploymentImpl"))
        .isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void operatonJerseyAutoConfigurationIsAbsent() {
    // This is the class that would build the JAX-RS endpoints. Not depending on
    // operaton-bpm-spring-boot-starter-rest is what keeps it away.
    assertThatThrownBy(() -> Class.forName(
        "org.operaton.bpm.spring.boot.starter.rest.OperatonBpmRestJerseyAutoConfiguration"))
        .isInstanceOf(ClassNotFoundException.class);
  }
}
