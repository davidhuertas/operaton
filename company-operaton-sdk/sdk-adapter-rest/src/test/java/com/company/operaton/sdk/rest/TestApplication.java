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

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application used only to boot a test context. No manual component scan
 * or configuration is needed: {@code OperatonSdkAutoConfiguration} (sdk-engine-config) and
 * {@code SdkAdapterRestAutoConfiguration} (this module) are both picked up automatically via
 * their {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * registration - exactly what a real consuming app gets for free by having these jars on the
 * classpath.
 */
@SpringBootApplication
public class TestApplication {
}
