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

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.operaton.bpm.engine.rest.dto.ExceptionDto;
import org.operaton.bpm.engine.rest.exception.ExceptionHandlerHelper;

/**
 * Translates engine and REST exceptions into the same JSON payloads and status
 * codes the JAX-RS stack produced.
 *
 * <p>This replaces the JAX-RS {@code ExceptionMapper} providers
 * ({@code RestExceptionHandler}, {@code ProcessEngineExceptionHandler} and
 * friends), which cannot be used without a JAX-RS runtime. The mapping logic
 * itself is reused rather than reimplemented: {@code ExceptionHandlerHelper}
 * exposes both halves of it publicly.
 *
 * <p>Deliberately <em>not</em> used here is
 * {@code ExceptionHandlerHelper.getResponse(Throwable)}. It builds a
 * {@code jakarta.ws.rs.core.Response}, which resolves through
 * {@code RuntimeDelegate} and so needs a JAX-RS implementation on the
 * classpath. {@code fromException} and {@code getStatus} have no such
 * dependency — {@code Response.Status} is a plain enum.
 *
 * <p>The advice is scoped to {@link OperatonRestController} so it never
 * swallows exceptions from the rest of the application.
 */
@RestControllerAdvice(annotations = OperatonRestController.class)
public class OperatonRestExceptionAdvice {

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<ExceptionDto> handle(Throwable throwable) {
    return ResponseEntity
        .status(ExceptionHandlerHelper.getStatus(throwable).getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(ExceptionHandlerHelper.fromException(throwable));
  }
}
