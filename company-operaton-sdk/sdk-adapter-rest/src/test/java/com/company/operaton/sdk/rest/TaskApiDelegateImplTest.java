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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.task.Task;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Exercises the two Task endpoints over real HTTP against an embedded H2 engine - proves the
 * generated Spring MVC routing, the DTO binding to Operaton's own classes, and the delegate's
 * engine calls all work end to end, not just that the module compiles.
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TaskApiDelegateImplTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private TaskService taskService;

  private String taskId;

  @BeforeEach
  void createStandaloneTask() {
    Task task = taskService.newTask();
    task.setName("SDK POC task");
    taskService.saveTask(task);
    taskId = task.getId();
  }

  @AfterEach
  void cleanUp() {
    if (taskService.createTaskQuery().taskId(taskId).count() > 0) {
      taskService.deleteTask(taskId, true);
    }
  }

  @Test
  void queryTasksReturnsTheCreatedTask() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<List> response = restTemplate.exchange(
        "/task", org.springframework.http.HttpMethod.POST, new HttpEntity<>("{}", headers), List.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .anySatisfy(t -> assertThat(((Map<?, ?>) t).get("id")).isEqualTo(taskId));
  }

  @Test
  void completeRemovesTheTask() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Void> response = restTemplate.exchange(
        "/task/{id}/complete", org.springframework.http.HttpMethod.POST, new HttpEntity<>("{}", headers),
        Void.class, taskId);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(taskService.createTaskQuery().taskId(taskId).count()).isZero();
  }
}
