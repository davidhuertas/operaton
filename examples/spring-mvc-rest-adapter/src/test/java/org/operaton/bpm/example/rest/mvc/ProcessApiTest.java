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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the adapter end to end against a real engine: Spring MVC serves the
 * request, the JAX-RS resource implementation does the work.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProcessApiTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void listsProcessDefinitions() throws Exception {
    mockMvc.perform(get("/engine-rest/process-definition"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.key=='adapterTestProcess')]").exists());
  }

  @Test
  void countsProcessDefinitions() throws Exception {
    mockMvc.perform(get("/engine-rest/process-definition/count"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").isNumber());
  }

  /**
   * The important one for the {@code UriInfo} shim: these query parameters only
   * reach the engine query if {@code getQueryParameters()} feeds
   * {@code ProcessDefinitionQueryDto} correctly.
   */
  @Test
  void bindsQueryParametersIntoTheEngineQuery() throws Exception {
    mockMvc.perform(get("/engine-rest/process-definition").param("key", "adapterTestProcess"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    mockMvc.perform(get("/engine-rest/process-definition").param("key", "noSuchProcess"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void bindsSortingParameters() throws Exception {
    mockMvc.perform(get("/engine-rest/process-definition")
            .param("sortBy", "key")
            .param("sortOrder", "asc"))
        .andExpect(status().isOk());
  }

  /**
   * Starting an instance makes the delegate build a reflexive link through
   * {@code UriInfo.getBaseUriBuilder()}, so this covers {@link ServletUriBuilder}.
   */
  @Test
  void startsProcessInstanceAndBuildsSelfLink() throws Exception {
    mockMvc.perform(post("/engine-rest/process-definition/key/adapterTestProcess/start")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.links[0].href").value(
            org.hamcrest.Matchers.containsString("/engine-rest/process-instance/")));
  }

  @Test
  void listsAndDeletesProcessInstances() throws Exception {
    String id = startInstance();

    mockMvc.perform(get("/engine-rest/process-instance/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));

    mockMvc.perform(delete("/engine-rest/process-instance/" + id))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/engine-rest/process-instance/" + id))
        .andExpect(status().isNotFound());
  }

  @Test
  void listsTasksOfAStartedInstance() throws Exception {
    String id = startInstance();

    mockMvc.perform(get("/engine-rest/task").param("processInstanceId", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Review"));
  }

  /**
   * Claiming goes through {@code TaskResourceImpl}, which only class-loads
   * because {@link MinimalRuntimeDelegate} supplies
   * {@code createVariantListBuilder()} for its static {@code VARIANTS} field.
   */
  @Test
  void claimsAndUnclaimsATask() throws Exception {
    String taskId = firstTaskOf(startInstance());

    mockMvc.perform(post("/engine-rest/task/" + taskId + "/claim")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":\"demo\"}"))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/engine-rest/task").param("taskId", taskId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].assignee").value("demo"));

    mockMvc.perform(post("/engine-rest/task/" + taskId + "/unclaim"))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/engine-rest/task").param("taskId", taskId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].assignee").doesNotExist());
  }

  @Test
  void countsTasks() throws Exception {
    startInstance();

    mockMvc.perform(get("/engine-rest/task/count"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").isNumber());
  }

  /**
   * Errors must carry Operaton's own {@code ExceptionDto} payload and status,
   * which is what {@link OperatonRestExceptionAdvice} reuses from
   * {@code ExceptionHandlerHelper} — without ever building a JAX-RS
   * {@code Response}.
   */
  @Test
  void reportsEngineErrorsInTheOperatonExceptionFormat() throws Exception {
    mockMvc.perform(get("/engine-rest/process-instance/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").isNotEmpty())
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  /** POSTs without a body must work, which is what {@code EmptyBodyFilter} is for. */
  @Test
  void acceptsPostWithoutABody() throws Exception {
    mockMvc.perform(post("/engine-rest/process-definition/key/adapterTestProcess/start")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNotEmpty());
  }

  private String firstTaskOf(String processInstanceId) throws Exception {
    String body = mockMvc.perform(get("/engine-rest/task").param("processInstanceId", processInstanceId))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return com.jayway.jsonpath.JsonPath.read(body, "$[0].id");
  }

  private String startInstance() throws Exception {
    String body = mockMvc.perform(post("/engine-rest/process-definition/key/adapterTestProcess/start")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return com.jayway.jsonpath.JsonPath.read(body, "$.id");
  }
}
