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
package org.operaton.bpm.example.rest.mvc.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.operaton.bpm.engine.rest.ProcessDefinitionRestService;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.StatisticsResultDto;
import org.operaton.bpm.engine.rest.dto.repository.ProcessDefinitionDiagramDto;
import org.operaton.bpm.engine.rest.dto.repository.ProcessDefinitionDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.StartProcessInstanceDto;
import org.operaton.bpm.example.rest.mvc.OperatonRestController;
import org.operaton.bpm.example.rest.mvc.RestServiceFactory;
import org.operaton.bpm.example.rest.mvc.ServletUriInfo;

/**
 * Spring MVC front for {@link ProcessDefinitionRestService}.
 *
 * <p>Each handler constructs the JAX-RS delegate and calls it. The base path is
 * applied globally, so the mappings here are relative to it — the endpoints land
 * on {@code /engine-rest/process-definition/...} by default.
 */
@OperatonRestController
@RequestMapping(ProcessDefinitionRestService.PATH)
public class ProcessDefinitionController {

  private final RestServiceFactory services;

  public ProcessDefinitionController(RestServiceFactory services) {
    this.services = services;
  }

  private ServletUriInfo uriInfo(HttpServletRequest request) {
    return new ServletUriInfo(request, services.basePath());
  }

  @GetMapping
  public List<ProcessDefinitionDto> getProcessDefinitions(HttpServletRequest request,
                                                          @RequestParam(required = false) Integer firstResult,
                                                          @RequestParam(required = false) Integer maxResults) {
    return services.processDefinitionService()
        .getProcessDefinitions(uriInfo(request), firstResult, maxResults);
  }

  @GetMapping("/count")
  public CountResultDto getProcessDefinitionsCount(HttpServletRequest request) {
    return services.processDefinitionService().getProcessDefinitionsCount(uriInfo(request));
  }

  @GetMapping("/statistics")
  public List<StatisticsResultDto> getStatistics(@RequestParam(required = false) Boolean failedJobs,
                                                 @RequestParam(required = false) Boolean rootIncidents,
                                                 @RequestParam(required = false) Boolean incidents,
                                                 @RequestParam(required = false) String incidentsForType) {
    return services.processDefinitionService()
        .getStatistics(failedJobs, rootIncidents, incidents, incidentsForType);
  }

  @GetMapping("/{id}")
  public ProcessDefinitionDto getProcessDefinition(@PathVariable String id) {
    return services.processDefinitionService().getProcessDefinitionById(id).getProcessDefinition();
  }

  @GetMapping("/{id}/xml")
  public ProcessDefinitionDiagramDto getProcessDefinitionBpmn20Xml(@PathVariable String id) {
    return services.processDefinitionService()
        .getProcessDefinitionById(id)
        .getProcessDefinitionBpmn20Xml();
  }

  /**
   * Starts a process instance.
   *
   * <p>This is the endpoint that exercises {@code ServletUriInfo.getBaseUriBuilder()}:
   * the delegate builds the new instance's {@code self} link from it.
   */
  @PostMapping("/{id}/start")
  public ProcessInstanceDto startProcessInstance(HttpServletRequest request,
                                                 @PathVariable String id,
                                                 @RequestBody StartProcessInstanceDto parameters) {
    return services.processDefinitionService()
        .getProcessDefinitionById(id)
        .startProcessInstance(uriInfo(request), parameters);
  }

  @GetMapping("/key/{key}")
  public ProcessDefinitionDto getProcessDefinitionByKey(@PathVariable String key) {
    return services.processDefinitionService().getProcessDefinitionByKey(key).getProcessDefinition();
  }

  @PostMapping("/key/{key}/start")
  public ProcessInstanceDto startProcessInstanceByKey(HttpServletRequest request,
                                                      @PathVariable String key,
                                                      @RequestBody StartProcessInstanceDto parameters) {
    return services.processDefinitionService()
        .getProcessDefinitionByKey(key)
        .startProcessInstance(uriInfo(request), parameters);
  }
}
