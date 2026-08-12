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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code operaton.rest-mvc.base-path} plays the role {@code spring.jersey.application-path}
 * played for the Jersey stack: it moves the whole API in one setting.
 */
@SpringBootTest(properties = "operaton.rest-mvc.base-path=/api")
@AutoConfigureMockMvc
class CustomBasePathTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void servesTheApiUnderTheConfiguredBasePath() throws Exception {
    mockMvc.perform(get("/api/process-definition"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.key=='adapterTestProcess')]").exists());
  }

  @Test
  void noLongerServesTheDefaultBasePath() throws Exception {
    mockMvc.perform(get("/engine-rest/process-definition"))
        .andExpect(status().isNotFound());
  }

  /** Generated links must follow the base path too, not hard-code /engine-rest. */
  @Test
  void generatesLinksUnderTheConfiguredBasePath() throws Exception {
    mockMvc.perform(post("/api/process-definition/key/adapterTestProcess/start")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.links[0].href").value(
            org.hamcrest.Matchers.containsString("/api/process-instance/")));
  }
}
