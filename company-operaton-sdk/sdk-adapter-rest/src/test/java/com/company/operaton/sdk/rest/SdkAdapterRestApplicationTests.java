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

import org.operaton.bpm.engine.ProcessEngine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
class SdkAdapterRestApplicationTests {

  @Autowired
  private ProcessEngine processEngine;

  @Autowired
  private TaskApiDelegateImpl taskApiDelegateImpl;

  @Test
  void contextLoads() {
    assertThat(processEngine).isNotNull();
    assertThat(taskApiDelegateImpl).isNotNull();
  }
}
