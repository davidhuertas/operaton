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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import org.operaton.bpm.engine.rest.ProcessDefinitionRestService;
import org.operaton.bpm.engine.rest.ProcessInstanceRestService;
import org.operaton.bpm.engine.rest.TaskRestService;
import org.operaton.bpm.engine.rest.impl.ProcessDefinitionRestServiceImpl;
import org.operaton.bpm.engine.rest.impl.ProcessInstanceRestServiceImpl;
import org.operaton.bpm.engine.rest.impl.TaskRestServiceImpl;

/**
 * Builds the JAX-RS resource services the controllers delegate to.
 *
 * <p>The resource implementations are ordinary objects with a uniform public
 * {@code (String engineName, ObjectMapper objectMapper)} constructor — the same
 * way {@code AbstractProcessEngineRestServiceImpl} builds them — so no JAX-RS
 * machinery is involved in creating one. They are cheap to construct and are
 * created per request by the JAX-RS runtime too, so this factory does the same
 * rather than caching them.
 *
 * <p>Return types are the {@code *RestService} interfaces rather than the
 * implementation classes, which keeps controllers decoupled from how the
 * delegate is obtained.
 */
@Component
public class RestServiceFactory {

  private final ObjectMapper objectMapper;
  private final OperatonRestMvcProperties properties;

  public RestServiceFactory(ObjectMapper objectMapper, OperatonRestMvcProperties properties) {
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  public ProcessDefinitionRestService processDefinitionService() {
    return new ProcessDefinitionRestServiceImpl(properties.getEngineName(), objectMapper);
  }

  public ProcessInstanceRestService processInstanceService() {
    return new ProcessInstanceRestServiceImpl(properties.getEngineName(), objectMapper);
  }

  public TaskRestService taskService() {
    return new TaskRestServiceImpl(properties.getEngineName(), objectMapper);
  }

  public ObjectMapper objectMapper() {
    return objectMapper;
  }

  public String basePath() {
    return properties.getBasePath();
  }
}
