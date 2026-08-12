# Spring MVC REST adapter

A worked example of serving the Operaton REST API from Spring MVC
`@RestController`s that delegate to the existing JAX-RS resource
implementations — with **no JAX-RS endpoint ever built or started**.

It answers a specific question: *how do I keep all the JAX-RS classes but stop
their endpoints from being deployed, so only my Spring MVC controllers serve
HTTP?*

## The short answer

You don't have to disable anything. JAX-RS annotations are inert metadata — they
do nothing unless a JAX-RS runtime scans and registers them.
`operaton-engine-rest-core` depends on `jakarta.ws.rs-api` (the annotations and
interfaces) and on **no JAX-RS implementation**.

In a Spring Boot app the endpoints come alive through exactly one chain, all of
it inside `operaton-bpm-spring-boot-starter-rest`:

1. the starter pulls in `spring-boot-starter-jersey`;
2. its `AutoConfiguration.imports` registers `OperatonBpmRestJerseyAutoConfiguration`;
3. that creates `OperatonJerseyResourceConfig` — a Jersey `ResourceConfig`
   annotated `@ApplicationPath("/engine-rest")` — which calls
   `registerClasses(OperatonRestResources.getResourceClasses())`;
4. Spring Boot's `JerseyAutoConfiguration` sees the `ResourceConfig` bean and
   registers the Jersey servlet.

**Break it at step 1.** This module depends on `operaton-bpm-spring-boot-starter`
plus `operaton-engine-rest-core`, and never on `-starter-rest`. Every JAX-RS
resource class, DTO and converter stays available to delegate to; no Jersey jar,
no auto-config, no servlet, no endpoints.

`pom.xml` also carries a `maven-enforcer-plugin` `bannedDependencies` rule, so a
transitive dependency cannot quietly put Jersey back. `NoJaxRsRuntimeTest` asserts
the same thing at runtime.

## What you have to re-provide

Dropping `-starter-rest` loses three things that are not Jersey-specific:

| What | Status here | Why |
|---|---|---|
| `ProcessEngineProvider` SPI | **Required** — `spi/SpringMvcProcessEngineProvider` + `META-INF/services` | Every resource impl calls `EngineUtil.lookupProcessEngine()`, which `ServiceLoader`-looks it up and throws without it |
| `EmptyBodyFilter`, `CacheControlFilter` | Registered in `OperatonRestMvcConfiguration` | Plain servlet filters, not JAX-RS providers, so they work unchanged |
| Fetch-and-lock long polling | **Not implemented** | Built on JAX-RS `@Suspended AsyncResponse`; bridging it to `DeferredResult` is separate work |

## How it is put together

| Class | Role |
|---|---|
| `OperatonRestMvcProperties` | `operaton.rest-mvc.base-path` (default `/engine-rest`) and `engine-name` |
| `OperatonRestMvcConfiguration` | Applies the base path, registers the filters, configures Jackson the way Operaton does |
| `OperatonRestController` | Marker annotation scoping the prefix and the exception advice to adapter controllers |
| `RestServiceFactory` | Builds the JAX-RS delegates, typed to the `*RestService` interfaces |
| `ServletUriInfo` | `UriInfo` over `HttpServletRequest` — query binding and link generation |
| `ServletUriBuilder` | `UriBuilder` over Spring's `UriComponentsBuilder` |
| `MinimalRuntimeDelegate` | The bare `RuntimeDelegate` some resource classes need just to load — see below |
| `OperatonRestExceptionAdvice` | Operaton's own error payloads, via `ExceptionHandlerHelper` |
| `controller/*` | The adapted endpoints |

One build detail worth copying: the pom sets `<parameters>true</parameters>` on
`maven-compiler-plugin`. `spring-boot-starter-parent` does this for you, but this
module inherits Operaton's parent, and without it every handler fails at runtime
with *"Name for argument of type [java.lang.String] not specified"*.

The delegates are ordinary objects with a uniform public
`(String engineName, ObjectMapper objectMapper)` constructor — the same way
`AbstractProcessEngineRestServiceImpl` builds them — so constructing one involves
no JAX-RS machinery. JAX-RS `@Path` sub-resource locators are just called as the
plain Java methods they are.

### The base path

`operaton.rest-mvc.base-path` plays the role `spring.jersey.application-path`
played for Jersey: it moves the whole API in one setting. It is applied as a
single prefix via `PathMatchConfigurer.addPathPrefix`, so controllers declare
only their own path and nothing else in the application is affected.

```yaml
operaton:
  rest-mvc:
    base-path: /api
```

## The `RuntimeDelegate` constraint

Some JAX-RS types are not plain objects: `Response.status(...)`,
`UriBuilder.fromPath(...)` and `Variant.mediaTypes(...)` all resolve through
`jakarta.ws.rs.ext.RuntimeDelegate`, which `ServiceLoader`-looks-up a JAX-RS
implementation and fails when none is present.

The part that is easy to miss — and that this example ran into — is that this is
not only about *calling* such methods. `TaskRestServiceImpl` and
`TaskResourceImpl` each hold a **static** field:

```java
private static final List<Variant> VARIANTS =
    Variant.mediaTypes(APPLICATION_JSON_TYPE, APPLICATION_HAL_JSON_TYPE).add().build();
```

so without a `RuntimeDelegate` those classes fail to **class-load** at all —
`ExceptionInInitializerError`, before any method runs, no matter which method you
were trying to reach. The entire `/task` surface is unreachable, not just the
HAL-negotiating parts.

`MinimalRuntimeDelegate` therefore implements exactly the two factories that are
reachable, and throws with an explanatory message for the rest:

- `createVariantListBuilder()` — what those static initializers need;
- `createUriBuilder()` — so the static `UriBuilder.fromPath(...)` used by HAL
  relations works.

Registering it is a JVM-wide decision, which is safe here precisely because the
enforcer rule bans any real JAX-RS implementation from the classpath.

Beyond that, the adapter avoids `Response` rather than implementing it:

- Most service methods return DTOs (`List<ProcessDefinitionDto>`,
  `CountResultDto`, `void`) and need nothing special.
- `UriInfo.getBaseUriBuilder()` is implemented by `ServletUriBuilder`, so
  reflexive links (the `self` link on a started instance) work.
- Errors use `ExceptionHandlerHelper.fromException` + `getStatus` rather than
  `getResponse`, neither of which builds a `Response`.

**Still not adapted here**, and what each would take:

| Endpoint | Blocker |
|---|---|
| `POST /task/{id}/complete`, `submit`, diagrams, deployment downloads | delegate returns `Response` → implement `createResponseBuilder()` plus a `Response` subclass |
| `GET /task/{id}` | `getTask(@Context Request)` negotiates JSON vs HAL → needs a `jakarta.ws.rs.core.Request` shim |
| `POST /external-task/fetchAndLock` (long polling) | `@Suspended AsyncResponse` → bridge to Spring's `DeferredResult` |

All ~40 `Response` call sites in `engine-rest` use simple `Response.ok(entity)` /
`noContent()` / `status(...)` patterns, so the first row is bounded work — just
more than this example needs.

## Coverage

Deliberately a subset — process definition, process instance and task — chosen to
exercise every part of the mechanism (query binding, sub-resource locators, link
generation, error mapping). The full API is roughly 390 HTTP-annotated methods
across 33 `*RestService` interfaces; at that scale, generate the controllers from
`engine-rest/engine-rest-openapi` (openapi-generator's `spring` generator with
`interfaceOnly=true`) and implement the generated interfaces with the same
delegation. That spec is hand-maintained rather than derived from the Java
annotations, so verify generated signatures against the resource classes.

## Running the tests

```bash
./mvnw test -pl examples/spring-mvc-rest-adapter -am
```
