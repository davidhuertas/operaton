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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.springframework.web.servlet.HandlerMapping;

/**
 * A {@link UriInfo} backed by the current {@link HttpServletRequest}.
 *
 * <p>This is the one real piece of plumbing the adapter needs. Operaton's
 * resource implementations take {@code @Context UriInfo} for two purposes:
 *
 * <ul>
 *   <li>binding query parameters — {@code AbstractQueryDto} is constructed from
 *       {@code uriInfo.getQueryParameters()}, which is how {@code sortBy},
 *       {@code sortOrder} and every filter criterion reach the engine query;</li>
 *   <li>generating reflexive links — via {@link #getBaseUriBuilder()}.</li>
 * </ul>
 *
 * <p>Both are implemented. {@code MultivaluedHashMap} ships in
 * {@code jakarta.ws.rs-api} itself, so building the query map needs no JAX-RS
 * runtime; uri building is handled by {@link ServletUriBuilder}.
 */
public class ServletUriInfo implements UriInfo {

  private final HttpServletRequest request;
  private final String basePath;

  public ServletUriInfo(HttpServletRequest request, String basePath) {
    this.request = request;
    this.basePath = basePath;
  }

  /** Absolute prefix ({@code scheme://host:port}) of the current request. */
  private String origin() {
    URI url = URI.create(request.getRequestURL().toString());
    return url.getScheme() + "://" + url.getRawAuthority();
  }

  /** Context path plus the adapter's configured base path, e.g. {@code /engine-rest}. */
  private String basePrefix() {
    return request.getContextPath() + basePath;
  }

  @Override
  public String getPath() {
    return getPath(true);
  }

  @Override
  public String getPath(boolean decode) {
    String uri = request.getRequestURI();
    String prefix = basePrefix();
    String path = uri.startsWith(prefix) ? uri.substring(prefix.length()) : uri;
    return path.startsWith("/") ? path.substring(1) : path;
  }

  @Override
  public List<PathSegment> getPathSegments() {
    return getPathSegments(true);
  }

  @Override
  public List<PathSegment> getPathSegments(boolean decode) {
    List<PathSegment> segments = new ArrayList<>();
    for (String segment : getPath(decode).split("/")) {
      if (!segment.isEmpty()) {
        segments.add(new SimplePathSegment(segment));
      }
    }
    return segments;
  }

  @Override
  public URI getRequestUri() {
    String query = request.getQueryString();
    return URI.create(request.getRequestURL() + (query == null ? "" : "?" + query));
  }

  @Override
  public UriBuilder getRequestUriBuilder() {
    return ServletUriBuilder.fromUriString(getRequestUri().toString());
  }

  @Override
  public URI getAbsolutePath() {
    return URI.create(request.getRequestURL().toString());
  }

  @Override
  public UriBuilder getAbsolutePathBuilder() {
    return ServletUriBuilder.fromUriString(getAbsolutePath().toString());
  }

  @Override
  public URI getBaseUri() {
    return URI.create(origin() + basePrefix() + "/");
  }

  @Override
  public UriBuilder getBaseUriBuilder() {
    return ServletUriBuilder.fromUriString(origin() + basePrefix());
  }

  @Override
  public MultivaluedMap<String, String> getPathParameters() {
    return getPathParameters(true);
  }

  @Override
  @SuppressWarnings("unchecked")
  public MultivaluedMap<String, String> getPathParameters(boolean decode) {
    MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
    Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    if (attribute instanceof Map<?, ?> variables) {
      ((Map<String, String>) variables).forEach(result::putSingle);
    }
    return result;
  }

  @Override
  public MultivaluedMap<String, String> getQueryParameters() {
    return getQueryParameters(true);
  }

  @Override
  public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
    MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
    // The container has already decoded these. For the endpoints this adapter
    // serves it is equivalent to parsing the query string: Operaton's query
    // endpoints are GET, and its POST endpoints take a JSON body rather than a
    // form-encoded one, so no request body ever leaks into the parameter map.
    request.getParameterMap().forEach((name, values) -> result.put(name, Arrays.asList(values)));
    return result;
  }

  @Override
  public List<String> getMatchedURIs() {
    return getMatchedURIs(true);
  }

  @Override
  public List<String> getMatchedURIs(boolean decode) {
    // A JAX-RS runtime concept: the stack of matched resource URIs. Spring MVC
    // has no equivalent, and no Operaton resource reads it.
    return Collections.emptyList();
  }

  @Override
  public List<Object> getMatchedResources() {
    return Collections.emptyList();
  }

  @Override
  public String getMatchedResourceTemplate() {
    // The @Path template that matched, a JAX-RS runtime concept. Spring MVC's
    // equivalent is the best-matching-pattern request attribute, but no
    // Operaton resource reads this.
    return null;
  }

  @Override
  public URI resolve(URI uri) {
    return getBaseUri().resolve(uri);
  }

  @Override
  public URI relativize(URI uri) {
    return getRequestUri().relativize(resolve(uri));
  }

  private record SimplePathSegment(String path) implements PathSegment {

    @Override
    public String getPath() {
      return path;
    }

    @Override
    public MultivaluedMap<String, String> getMatrixParameters() {
      return new MultivaluedHashMap<>();
    }
  }
}
