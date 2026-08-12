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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * The smallest {@code RuntimeDelegate} that lets Operaton's resource classes be
 * used without a JAX-RS implementation.
 *
 * <p>It exists for a reason that is easy to miss: several resource classes
 * cannot even be <em>loaded</em> without one. {@code TaskRestServiceImpl} and
 * {@code TaskResourceImpl} both hold a static field
 *
 * <pre>{@code
 * private static final List<Variant> VARIANTS =
 *     Variant.mediaTypes(APPLICATION_JSON_TYPE, APPLICATION_HAL_JSON_TYPE).add().build();
 * }</pre>
 *
 * <p>{@code Variant.mediaTypes(...)} resolves through
 * {@code RuntimeDelegate.getInstance()}, so without this class every one of
 * those classes fails with {@code ExceptionInInitializerError} at class
 * initialization — before any method is called, and regardless of which method
 * you were trying to reach.
 *
 * <p>Only the two factories that are actually reachable are implemented:
 * {@code createVariantListBuilder()} (class loading, as above) and
 * {@code createUriBuilder()} (so the static {@code UriBuilder.fromPath(...)}
 * used by HAL relations works). The rest throw with an explanatory message
 * rather than pretending — see {@link ServletUriBuilder} for the same approach.
 *
 * <p>Registered via {@code META-INF/services/jakarta.ws.rs.ext.RuntimeDelegate}.
 * That is a JVM-wide registration, which is safe here precisely because the
 * build bans any real JAX-RS implementation from the classpath; if one were
 * present, it would be ambiguous which of the two won.
 */
public class MinimalRuntimeDelegate extends RuntimeDelegate {

  private static final String BOOTSTRAP_HINT =
      "SeBootstrap starts a standalone JAX-RS container, which is exactly what this module exists to avoid.";

  @Override
  public Variant.VariantListBuilder createVariantListBuilder() {
    return new SimpleVariantListBuilder();
  }

  @Override
  public UriBuilder createUriBuilder() {
    return new ServletUriBuilder(UriComponentsBuilder.newInstance());
  }

  @Override
  public Response.ResponseBuilder createResponseBuilder() {
    throw unsupported("createResponseBuilder",
        "Adapt endpoints whose delegate returns a JAX-RS Response by implementing "
            + "Response.ResponseBuilder and Response here. All ~40 call sites in engine-rest use "
            + "simple Response.ok(entity) / noContent() / status(...) patterns.");
  }

  @Override
  public Link.Builder createLinkBuilder() {
    throw unsupported("createLinkBuilder", "Only needed for JAX-RS Link headers.");
  }

  @Override
  public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) {
    throw unsupported("createHeaderDelegate", "Only needed to parse or format JAX-RS header types.");
  }

  @Override
  public jakarta.ws.rs.SeBootstrap.Configuration.Builder createConfigurationBuilder() {
    throw unsupported("createConfigurationBuilder", BOOTSTRAP_HINT);
  }

  @Override
  public java.util.concurrent.CompletionStage<jakarta.ws.rs.SeBootstrap.Instance> bootstrap(
      Application application, jakarta.ws.rs.SeBootstrap.Configuration configuration) {
    throw unsupported("bootstrap", BOOTSTRAP_HINT);
  }

  @Override
  public java.util.concurrent.CompletionStage<jakarta.ws.rs.SeBootstrap.Instance> bootstrap(
      Class<? extends Application> application, jakarta.ws.rs.SeBootstrap.Configuration configuration) {
    throw unsupported("bootstrap", BOOTSTRAP_HINT);
  }

  @Override
  public jakarta.ws.rs.core.EntityPart.Builder createEntityPartBuilder(String partName) {
    throw unsupported("createEntityPartBuilder", "Only needed for JAX-RS multipart handling.");
  }

  @Override
  public <T> T createEndpoint(Application application, Class<T> endpointType) {
    throw unsupported("createEndpoint",
        "This would publish JAX-RS endpoints, which is exactly what this module exists to avoid.");
  }

  private static UnsupportedOperationException unsupported(String operation, String hint) {
    return new UnsupportedOperationException(
        "RuntimeDelegate.%s is not implemented by the Spring MVC REST adapter. %s"
            .formatted(operation, hint));
  }

  /**
   * Builds variant lists as the cartesian product of the accumulated media
   * types, languages and encodings, per the {@code VariantListBuilder} contract.
   */
  static class SimpleVariantListBuilder extends Variant.VariantListBuilder {

    private final List<Variant> variants = new ArrayList<>();
    private final List<MediaType> mediaTypes = new ArrayList<>();
    private final List<Locale> languages = new ArrayList<>();
    private final List<String> encodings = new ArrayList<>();

    @Override
    public List<Variant> build() {
      if (!mediaTypes.isEmpty() || !languages.isEmpty() || !encodings.isEmpty()) {
        add();
      }
      return new ArrayList<>(variants);
    }

    @Override
    public Variant.VariantListBuilder add() {
      // A null in any dimension means "unspecified", which keeps the product
      // non-empty when only some dimensions were given.
      for (MediaType mediaType : orNull(mediaTypes)) {
        for (Locale language : orNull(languages)) {
          for (String encoding : orNull(encodings)) {
            variants.add(new Variant(mediaType, language, encoding));
          }
        }
      }
      mediaTypes.clear();
      languages.clear();
      encodings.clear();
      return this;
    }

    @Override
    public Variant.VariantListBuilder languages(Locale... languages) {
      this.languages.addAll(Arrays.asList(languages));
      return this;
    }

    @Override
    public Variant.VariantListBuilder encodings(String... encodings) {
      this.encodings.addAll(Arrays.asList(encodings));
      return this;
    }

    @Override
    public Variant.VariantListBuilder mediaTypes(MediaType... mediaTypes) {
      this.mediaTypes.addAll(Arrays.asList(mediaTypes));
      return this;
    }

    private static <T> List<T> orNull(List<T> values) {
      List<T> result = new ArrayList<>(values);
      if (result.isEmpty()) {
        result.add(null);
      }
      return result;
    }
  }
}
