/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.company.operaton.sdk.rest;

import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.dto.task.CompleteTaskDto;
import org.operaton.bpm.engine.rest.dto.task.TaskQueryDto;
import org.operaton.bpm.engine.rest.dto.task.TaskWithAttachmentAndCommentDto;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.variable.VariableMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.company.operaton.sdk.rest.generated.api.TaskApi;

/**
 * Implements the Task resource of the generated {@link TaskApi} (query + complete only - this
 * is a proof of concept, not the full Task resource). openapi-generator's "spring" generator at
 * 7.23.0 with {@code interfaceOnly=true} does not emit a separate {@code TaskApiDelegate}
 * interface; the overridable "delegate" methods live directly on {@link TaskApi} with a default
 * body that returns HTTP 501, so implementing {@link TaskApi} here and overriding only these two
 * methods is the equivalent pattern - every other Task operation keeps its default 501 response
 * until a real implementation is added.
 *
 * <p>The method bodies mirror what
 * {@code org.operaton.bpm.engine.rest.impl.TaskRestServiceImpl} and
 * {@code org.operaton.bpm.engine.rest.sub.task.impl.TaskResourceImpl} already do internally,
 * reusing Operaton's own DTO conversion logic ({@code TaskQueryDto.toQuery},
 * {@code TaskWithAttachmentAndCommentDto.fromEntity}, {@code VariableValueDto.toMap}/{@code
 * fromMap}) instead of re-deriving it - this is the whole point of binding the generated
 * interface to Operaton's real DTO classes rather than generator-minted duplicates.
 */
@RestController
public class TaskApiDelegateImpl implements TaskApi {

  private final ProcessEngine processEngine;
  private final ObjectMapper objectMapper;

  public TaskApiDelegateImpl(ProcessEngine processEngine, ObjectMapper objectMapper) {
    this.processEngine = processEngine;
    this.objectMapper = objectMapper;
  }

  @Override
  public ResponseEntity<List<TaskWithAttachmentAndCommentDto>> queryTasks(Integer firstResult, Integer maxResults,
      TaskQueryDto queryDto) {
    TaskQueryDto dto = queryDto != null ? queryDto : new TaskQueryDto();
    dto.setObjectMapper(objectMapper);
    TaskQuery query = dto.toQuery(processEngine);

    List<Task> matchingTasks = (firstResult != null || maxResults != null)
        ? query.listPage(firstResult == null ? 0 : firstResult, maxResults == null ? Integer.MAX_VALUE : maxResults)
        : query.list();

    // TaskWithAttachmentAndCommentDto.fromEntity(Task) is declared to return TaskDto (its
    // superclass) even though it always constructs a TaskWithAttachmentAndCommentDto instance -
    // TaskRestServiceImpl.queryTasks relies on the same static factory. The cast is safe.
    List<TaskWithAttachmentAndCommentDto> result = matchingTasks.stream()
        .map(task -> (TaskWithAttachmentAndCommentDto) TaskWithAttachmentAndCommentDto.fromEntity(task))
        .toList();

    return ResponseEntity.ok(result);
  }

  @Override
  public ResponseEntity<Map<String, VariableValueDto>> complete(String id, CompleteTaskDto completeTaskDto) {
    TaskService taskService = processEngine.getTaskService();
    CompleteTaskDto dto = completeTaskDto != null ? completeTaskDto : new CompleteTaskDto();
    VariableMap variables = VariableValueDto.toMap(dto.getVariables(), processEngine, objectMapper);

    if (dto.isWithVariablesInReturn()) {
      VariableMap resultVariables = taskService.completeWithVariablesInReturn(id, variables, false);
      return ResponseEntity.ok(VariableValueDto.fromMap(resultVariables, true));
    }

    taskService.complete(id, variables);
    return ResponseEntity.noContent().build();
  }
}
