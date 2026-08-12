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

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.operaton.bpm.engine.rest.ProcessInstanceRestService;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.runtime.ActivityInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceQueryDto;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceSuspensionStateDto;
import org.operaton.bpm.example.rest.mvc.OperatonRestController;
import org.operaton.bpm.example.rest.mvc.RestServiceFactory;
import org.operaton.bpm.example.rest.mvc.ServletUriInfo;

/**
 * Spring MVC front for {@link ProcessInstanceRestService} and the
 * {@code ProcessInstanceResource} sub-resource.
 *
 * <p>JAX-RS reaches the sub-resource through a {@code @Path} sub-resource
 * locator; here the locator method is simply called as the plain Java method it
 * is, and the result used directly.
 */
@OperatonRestController
@RequestMapping(ProcessInstanceRestService.PATH)
public class ProcessInstanceController {

  private final RestServiceFactory services;

  public ProcessInstanceController(RestServiceFactory services) {
    this.services = services;
  }

  private ServletUriInfo uriInfo(HttpServletRequest request) {
    return new ServletUriInfo(request, services.basePath());
  }

  @GetMapping
  public List<ProcessInstanceDto> getProcessInstances(HttpServletRequest request,
                                                      @RequestParam(required = false) Integer firstResult,
                                                      @RequestParam(required = false) Integer maxResults) {
    return services.processInstanceService()
        .getProcessInstances(uriInfo(request), firstResult, maxResults);
  }

  @PostMapping
  public List<ProcessInstanceDto> queryProcessInstances(@RequestBody ProcessInstanceQueryDto query,
                                                        @RequestParam(required = false) Integer firstResult,
                                                        @RequestParam(required = false) Integer maxResults) {
    return services.processInstanceService().queryProcessInstances(query, firstResult, maxResults);
  }

  @GetMapping("/count")
  public CountResultDto getProcessInstancesCount(HttpServletRequest request) {
    return services.processInstanceService().getProcessInstancesCount(uriInfo(request));
  }

  @PostMapping("/count")
  public CountResultDto queryProcessInstancesCount(@RequestBody ProcessInstanceQueryDto query) {
    return services.processInstanceService().queryProcessInstancesCount(query);
  }

  @GetMapping("/{id}")
  public ProcessInstanceDto getProcessInstance(@PathVariable String id) {
    return services.processInstanceService().getProcessInstance(id).getProcessInstance();
  }

  @GetMapping("/{id}/activity-instances")
  public ActivityInstanceDto getActivityInstanceTree(@PathVariable String id) {
    return services.processInstanceService().getProcessInstance(id).getActivityInstanceTree();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteProcessInstance(@PathVariable String id,
                                    @RequestParam(defaultValue = "false") boolean skipCustomListeners,
                                    @RequestParam(defaultValue = "false") boolean skipIoMappings,
                                    @RequestParam(defaultValue = "false") boolean skipSubprocesses,
                                    @RequestParam(defaultValue = "true") boolean failIfNotExists) {
    services.processInstanceService()
        .getProcessInstance(id)
        .deleteProcessInstance(skipCustomListeners, skipIoMappings, skipSubprocesses, failIfNotExists);
  }

  @PutMapping("/suspended")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateSuspensionState(@RequestBody ProcessInstanceSuspensionStateDto dto) {
    services.processInstanceService().updateSuspensionState(dto);
  }
}
