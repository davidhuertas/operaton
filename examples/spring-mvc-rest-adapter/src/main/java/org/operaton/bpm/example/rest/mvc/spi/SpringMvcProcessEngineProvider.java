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
package org.operaton.bpm.example.rest.mvc.spi;

import java.util.Set;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.rest.spi.ProcessEngineProvider;

/**
 * Makes the Spring-managed process engine discoverable to the JAX-RS resource
 * implementations this adapter delegates to.
 *
 * <p>This is not optional. Every resource implementation runs through
 * {@code AbstractRestProcessEngineAware}'s constructor, which calls
 * {@code EngineUtil.lookupProcessEngine(engineName)}. That performs a
 * {@link java.util.ServiceLoader} lookup for this SPI and throws
 * {@code RestException("Could not find an implementation of the
 * ProcessEngineProvider - SPI")} when nothing is registered.
 *
 * <p>{@code operaton-bpm-spring-boot-starter-rest} used to supply this, so
 * dropping that starter to get rid of Jersey means supplying it here instead.
 * Registration happens through
 * {@code META-INF/services/org.operaton.bpm.engine.rest.spi.ProcessEngineProvider}.
 */
public class SpringMvcProcessEngineProvider implements ProcessEngineProvider {

  @Override
  public ProcessEngine getDefaultProcessEngine() {
    return ProcessEngines.getDefaultProcessEngine();
  }

  @Override
  public ProcessEngine getProcessEngine(String name) {
    return ProcessEngines.getProcessEngine(name);
  }

  @Override
  public Set<String> getProcessEngineNames() {
    return ProcessEngines.getProcessEngines().keySet();
  }
}
