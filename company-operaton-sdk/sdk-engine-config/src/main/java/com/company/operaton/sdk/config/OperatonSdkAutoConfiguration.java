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
package com.company.operaton.sdk.config;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonProcessEngineConfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Applies the company's standard engine conventions on top of Operaton's Spring Boot starter
 * defaults. Registers a {@link OperatonProcessEngineConfiguration} bean, which the starter's
 * {@code processEngineConfigurationImpl} bean automatically collects (as a
 * {@code ProcessEnginePlugin}) and composes into the engine configuration - see
 * {@code OperatonBpmConfiguration#processEngineConfigurationImpl}.
 */
@AutoConfiguration
@EnableConfigurationProperties(OperatonSdkProperties.class)
public class OperatonSdkAutoConfiguration {

  @Bean
  public OperatonProcessEngineConfiguration companyStandardEngineConfiguration(OperatonSdkProperties properties) {
    return new OperatonProcessEngineConfiguration() {
      @Override
      public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setHistory(properties.getHistoryLevel());
        processEngineConfiguration.setDatabaseSchemaUpdate(properties.getDatabaseSchemaUpdate());
        processEngineConfiguration.setJobExecutorActivate(properties.isJobExecutorActivate());
      }
    };
  }
}
