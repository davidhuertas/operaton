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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.HandlerTypePredicate;

import org.operaton.bpm.engine.rest.filter.CacheControlFilter;
import org.operaton.bpm.engine.rest.filter.EmptyBodyFilter;
import org.operaton.bpm.engine.rest.mapper.JacksonConfigurator;

/**
 * Wires up the Spring MVC REST adapter.
 *
 * <p>Note what is <em>not</em> here: nothing registers a JAX-RS application,
 * resource config or servlet. The JAX-RS classes this adapter delegates to are
 * plain objects on the classpath; without {@code operaton-bpm-spring-boot-starter-rest}
 * and its Jersey dependency, no JAX-RS endpoint is ever built or started.
 */
@Configuration
@EnableConfigurationProperties(OperatonRestMvcProperties.class)
@ComponentScan(basePackageClasses = OperatonRestMvcConfiguration.class)
public class OperatonRestMvcConfiguration implements WebMvcConfigurer {

  private final OperatonRestMvcProperties properties;

  public OperatonRestMvcConfiguration(OperatonRestMvcProperties properties) {
    this.properties = properties;
  }

  /**
   * Serves every adapter controller under the configured base path, the way
   * {@code spring.jersey.application-path} does for the Jersey stack.
   *
   * <p>Applying it as a single prefix keeps the base path out of the individual
   * {@code @RequestMapping}s, and scoping it to {@link OperatonRestController}
   * keeps it away from any other controller in the application.
   */
  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(properties.getBasePath(),
        HandlerTypePredicate.forAnnotation(OperatonRestController.class));
  }

  /**
   * Uses Operaton's own Jackson setup — notably the
   * {@code yyyy-MM-dd'T'HH:mm:ss.SSSZ} date format — so DTOs serialize exactly
   * as they did through JAX-RS.
   *
   * <p>Defining this bean makes Spring Boot's Jackson auto-configuration back
   * off, so it applies both to the delegates and to HTTP message conversion.
   */
  @Bean
  public ObjectMapper objectMapper() {
    return JacksonConfigurator.configureObjectMapper(new ObjectMapper());
  }

  /**
   * Substitutes an empty JSON object for an absent request body, so bodyless
   * POSTs behave as REST API clients expect. A plain servlet filter, not a
   * JAX-RS provider, so it works unchanged under Spring MVC.
   */
  @Bean
  public FilterRegistrationBean<EmptyBodyFilter> emptyBodyFilter() {
    return filter(new EmptyBodyFilter(), "EmptyBodyFilter");
  }

  /** Applies the REST API's no-cache headers. Also a plain servlet filter. */
  @Bean
  public FilterRegistrationBean<CacheControlFilter> cacheControlFilter() {
    return filter(new CacheControlFilter(), "CacheControlFilter");
  }

  private <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> filter(T filter, String name) {
    FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
    registration.setName(name);
    registration.addUrlPatterns(properties.getBasePath() + "/*");
    return registration;
  }
}
