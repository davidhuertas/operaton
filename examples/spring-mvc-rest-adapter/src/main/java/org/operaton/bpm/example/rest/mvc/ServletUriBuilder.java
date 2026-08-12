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

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import jakarta.ws.rs.core.UriBuilder;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * A {@link UriBuilder} backed by Spring's {@link UriComponentsBuilder}.
 *
 * <p>This exists because {@code UriBuilder} is not a plain object: its static
 * factories ({@code UriBuilder.fromPath}, {@code fromUri}, …) resolve through
 * {@code jakarta.ws.rs.ext.RuntimeDelegate}, which requires a JAX-RS
 * implementation on the classpath. Since the whole point of this module is that
 * there isn't one, {@link ServletUriInfo} hands out instances of this class
 * instead of calling those factories.
 *
 * <p>Resource implementations use uri builders to generate reflexive links —
 * for example {@code ProcessDefinitionResourceImpl.startProcessInstance} builds
 * the {@code self} link of the new instance with
 * {@code context.getBaseUriBuilder().path(...).build()}. Those methods are
 * implemented here. The parts of the {@code UriBuilder} contract that only a
 * real JAX-RS runtime can satisfy — resolving {@code @Path} annotations off a
 * resource class or method — throw {@link UnsupportedOperationException}; no
 * Operaton resource reaches them through this path.
 */
class ServletUriBuilder extends UriBuilder {

  private final UriComponentsBuilder delegate;

  ServletUriBuilder(UriComponentsBuilder delegate) {
    this.delegate = delegate;
  }

  static ServletUriBuilder fromUriString(String uri) {
    return new ServletUriBuilder(UriComponentsBuilder.fromUriString(uri));
  }

  @Override
  public UriBuilder clone() {
    return new ServletUriBuilder(delegate.cloneBuilder());
  }

  @Override
  public UriBuilder uri(URI uri) {
    delegate.uri(uri);
    return this;
  }

  @Override
  public UriBuilder uri(String uriTemplate) {
    return new ServletUriBuilder(UriComponentsBuilder.fromUriString(uriTemplate));
  }

  @Override
  public UriBuilder scheme(String scheme) {
    delegate.scheme(scheme);
    return this;
  }

  @Override
  public UriBuilder userInfo(String ui) {
    delegate.userInfo(ui);
    return this;
  }

  @Override
  public UriBuilder host(String host) {
    delegate.host(host);
    return this;
  }

  @Override
  public UriBuilder port(int port) {
    delegate.port(port);
    return this;
  }

  @Override
  public UriBuilder replacePath(String path) {
    delegate.replacePath(path);
    return this;
  }

  @Override
  public UriBuilder path(String path) {
    // Resource implementations chain path() calls that include the resource's
    // relative root, which is "/" for the default engine. Appending that
    // verbatim would produce a doubled slash, so treat it as a no-op.
    if (path == null || path.isEmpty() || "/".equals(path)) {
      return this;
    }
    delegate.path(path.startsWith("/") ? path : "/" + path);
    return this;
  }

  @Override
  public UriBuilder segment(String... segments) {
    delegate.pathSegment(segments);
    return this;
  }

  @Override
  public UriBuilder replaceQuery(String query) {
    delegate.replaceQuery(query);
    return this;
  }

  @Override
  public UriBuilder queryParam(String name, Object... values) {
    delegate.queryParam(name, values);
    return this;
  }

  @Override
  public UriBuilder replaceQueryParam(String name, Object... values) {
    delegate.replaceQueryParam(name, values);
    return this;
  }

  @Override
  public UriBuilder fragment(String fragment) {
    delegate.fragment(fragment);
    return this;
  }

  @Override
  public URI build(Object... values) {
    return delegate.build().expand(values).encode().toUri();
  }

  @Override
  public URI build(Object[] values, boolean encodeSlashInPath) {
    return build(values);
  }

  @Override
  public URI buildFromEncoded(Object... values) {
    return delegate.build(true).expand(values).toUri();
  }

  @Override
  public URI buildFromMap(Map<String, ?> values) {
    return delegate.build().expand(values).encode().toUri();
  }

  @Override
  public URI buildFromMap(Map<String, ?> values, boolean encodeSlashInPath) {
    return buildFromMap(values);
  }

  @Override
  public URI buildFromEncodedMap(Map<String, ?> values) {
    return delegate.build(true).expand(values).toUri();
  }

  @Override
  public UriBuilder resolveTemplate(String name, Object value) {
    return resolveTemplates(Map.of(name, value));
  }

  @Override
  public UriBuilder resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
    return resolveTemplates(Map.of(name, value));
  }

  @Override
  public UriBuilder resolveTemplateFromEncoded(String name, Object value) {
    return resolveTemplates(Map.of(name, value));
  }

  @Override
  public UriBuilder resolveTemplates(Map<String, Object> templateValues) {
    return new ServletUriBuilder(
        UriComponentsBuilder.fromUri(delegate.build().expand(templateValues).toUri()));
  }

  @Override
  public UriBuilder resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) {
    return resolveTemplates(templateValues);
  }

  @Override
  public UriBuilder resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
    return resolveTemplates(templateValues);
  }

  @Override
  public String toTemplate() {
    return delegate.build().toUriString();
  }

  // --- Parts of the contract that need a real JAX-RS runtime -----------------

  @Override
  public UriBuilder schemeSpecificPart(String ssp) {
    throw unsupported("schemeSpecificPart");
  }

  @Override
  public UriBuilder path(@SuppressWarnings("rawtypes") Class resource) {
    throw unsupported("path(Class)");
  }

  @Override
  public UriBuilder path(@SuppressWarnings("rawtypes") Class resource, String method) {
    throw unsupported("path(Class, String)");
  }

  @Override
  public UriBuilder path(Method method) {
    throw unsupported("path(Method)");
  }

  @Override
  public UriBuilder replaceMatrix(String matrix) {
    throw unsupported("replaceMatrix");
  }

  @Override
  public UriBuilder matrixParam(String name, Object... values) {
    throw unsupported("matrixParam");
  }

  @Override
  public UriBuilder replaceMatrixParam(String name, Object... values) {
    throw unsupported("replaceMatrixParam");
  }

  private static UnsupportedOperationException unsupported(String operation) {
    return new UnsupportedOperationException(
        "UriBuilder.%s is not supported by the Spring MVC REST adapter, because it requires a JAX-RS runtime. "
            .formatted(operation)
            + "If you hit this, the endpoint you adapted needs either a different delegate method or a "
            + "jakarta.ws.rs.ext.RuntimeDelegate implementation on the classpath.");
  }
}
