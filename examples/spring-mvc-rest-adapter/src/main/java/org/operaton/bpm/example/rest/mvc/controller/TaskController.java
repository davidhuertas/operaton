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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.operaton.bpm.engine.rest.TaskRestService;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.task.CompleteTaskDto;
import org.operaton.bpm.engine.rest.dto.task.TaskDto;
import org.operaton.bpm.engine.rest.dto.task.TaskQueryDto;
import org.operaton.bpm.engine.rest.dto.task.UserIdDto;
import org.operaton.bpm.example.rest.mvc.OperatonRestController;
import org.operaton.bpm.example.rest.mvc.RestServiceFactory;
import org.operaton.bpm.example.rest.mvc.ServletUriInfo;

/**
 * Spring MVC front for {@link TaskRestService} and the {@code TaskResource}
 * sub-resource.
 */
@OperatonRestController
@RequestMapping(TaskRestService.PATH)
public class TaskController {

  private final RestServiceFactory services;

  public TaskController(RestServiceFactory services) {
    this.services = services;
  }

  private ServletUriInfo uriInfo(HttpServletRequest request) {
    return new ServletUriInfo(request, services.basePath());
  }

  /**
   * Lists tasks.
   *
   * <p>The JAX-RS method behind this one is
   * {@code getTasks(@Context Request, @Context UriInfo, ...)}, which returns
   * {@code Object} and negotiates between JSON and HAL using the JAX-RS
   * {@code Request}. Content negotiation is a JAX-RS runtime facility, so
   * instead of shimming {@code Request} this binds the query parameters into a
   * {@link TaskQueryDto} — exactly what the JSON branch of that method does —
   * and calls {@code queryTasks}. Same result, plain JSON, no runtime needed.
   */
  @GetMapping
  public List<TaskDto> getTasks(HttpServletRequest request,
                                @RequestParam(required = false) Integer firstResult,
                                @RequestParam(required = false) Integer maxResults) {
    TaskQueryDto query =
        new TaskQueryDto(services.objectMapper(), uriInfo(request).getQueryParameters());
    return services.taskService().queryTasks(query, firstResult, maxResults);
  }

  @PostMapping
  public List<TaskDto> queryTasks(@RequestBody TaskQueryDto query,
                                  @RequestParam(required = false) Integer firstResult,
                                  @RequestParam(required = false) Integer maxResults) {
    return services.taskService().queryTasks(query, firstResult, maxResults);
  }

  @GetMapping("/count")
  public CountResultDto getTasksCount(HttpServletRequest request) {
    return services.taskService().getTasksCount(uriInfo(request));
  }

  @PostMapping("/count")
  public CountResultDto queryTasksCount(@RequestBody TaskQueryDto query) {
    return services.taskService().queryTasksCount(query);
  }

  // Two single-task operations are missing here, for two different reasons.
  //
  // GET /task/{id} maps to TaskResource.getTask(@Context Request), which
  // negotiates between JSON and HAL via request.selectVariant(). That needs a
  // jakarta.ws.rs.core.Request shim implementing Accept-header negotiation.
  //
  // POST /task/{id}/complete returns a JAX-RS Response, so it needs
  // MinimalRuntimeDelegate.createResponseBuilder() -- see the note there.
  //
  // Everything on this class, and claim/unclaim/resolve below, only needed
  // MinimalRuntimeDelegate.createVariantListBuilder(): TaskRestServiceImpl and
  // TaskResourceImpl both hold a *static* VARIANTS field built with
  // Variant.mediaTypes(...), so without it they fail to class-load entirely --
  // ExceptionInInitializerError, before any method runs.

  @PostMapping("/{id}/claim")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void claim(@PathVariable String id, @RequestBody UserIdDto dto) {
    services.taskService().getTask(id, false, false, false).claim(dto);
  }

  @PostMapping("/{id}/unclaim")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unclaim(@PathVariable String id) {
    services.taskService().getTask(id, false, false, false).unclaim();
  }

  @PostMapping("/{id}/resolve")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resolve(@PathVariable String id, @RequestBody CompleteTaskDto dto) {
    services.taskService().getTask(id, false, false, false).resolve(dto);
  }
}
