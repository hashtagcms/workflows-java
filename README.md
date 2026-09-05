<p align="center">
  <img src="art/logo.png" alt="HashtagCMS Workflows" width="440">
</p>

<h1 align="center">HashtagCMS Workflows — Java (Spring Boot)</h1>

<p align="center">
  <a href="https://central.sonatype.com/artifact/org.hashtagcms/workflows"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/org.hashtagcms/workflows.svg?label=Maven%20Central"></a>
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-8B5CF6.svg"></a>
  <a href="https://adoptium.net/"><img alt="Java 21+" src="https://img.shields.io/badge/Java-21%2B-blue.svg"></a>
  <a href="https://spring.io/projects/spring-boot"><img alt="Spring Boot 3" src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F.svg"></a>
</p>

A server-driven **workflow & action orchestration engine** for HashtagCMS,
packaged as a **Spring Boot auto-configuration library** (API only). It is a
functional port of the PHP `hashtagcms/workflows` service layer — the execution
engine, directive capability negotiation, persistence, and REST API. There is no
admin UI; management is done over REST.

Add it to any Spring Boot app and the whole engine wires itself up — or run it
standalone as a ready-made microservice.

- **Requirements:** Java 21+, Spring Boot 3.x. A JDBC datasource (the host app's
  choice; the standalone runner uses in-memory H2).
- **Documentation:** see the [`docs/`](docs/) guides — [Getting started](docs/getting-started.md),
  [Configuration](docs/configuration.md), [Docker](docs/docker.md),
  [Authentication & SSO](docs/authentication.md), [Extending](docs/extending.md),
  [Compatibility](docs/compatibility.md), [Publishing](docs/publishing.md).

## Installation

Add the dependency (JDBC driver is yours to choose):

**Maven**

```xml
<dependency>
  <groupId>org.hashtagcms</groupId>
  <artifactId>workflows</artifactId>
  <version>1.0.1</version>
</dependency>
```

**Gradle**

```groovy
implementation 'org.hashtagcms:workflows:1.0.1'
```

The artifact is on **Maven Central** — no extra repository config needed. Add a
**JDBC driver** too (the library ships none — it's your choice) and point Spring at
your database:

```xml
<dependency>
  <groupId>com.mysql</groupId>
  <artifactId>mysql-connector-j</artifactId>
  <scope>runtime</scope>
</dependency>
```

```yaml
# application.yml — any database Hibernate supports (MySQL, PostgreSQL, H2, …)
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/workflows
    username: app
    password: secret
  jpa:
    hibernate:
      ddl-auto: update      # update = Java owns the schema; validate = sharing PHP's
```

That's all the wiring there is. On startup the library's
[auto-configuration](src/main/java/org/hashtagcms/workflows/autoconfigure/WorkflowsAutoConfiguration.java)
registers the engine, the REST controllers, the directive manifest, seeding, and
the auth resolver against your application's `DataSource` — no `@Import`,
`@ComponentScan`, or `@EntityScan` on your side. It scans additively, so it never
disables your own entities or repositories. Every bean is
`@ConditionalOnMissingBean`, so you can override any of it (see
[Extending](docs/extending.md)). Configure it under `hashtagcms.workflows.*`
(see [Configuration](docs/configuration.md)).

## Run standalone

No global Maven needed — the **Maven Wrapper** (`./mvnw`) downloads it on first use.

```bash
./mvnw spring-boot:run
# or build the runnable jar (attached under the `exec` classifier):
./mvnw clean package && java -jar target/workflows-1.0.1-exec.jar
```

On startup it seeds the **72-directive manifest** and a few **example workflows**
(H2). The service listens on `http://localhost:8080`. (The plain
`target/workflows-1.0.1.jar` is the library artifact consumers depend on;
the `-exec.jar` is the self-contained runnable one.)

## Run with Docker

The image is published to **Docker Hub** as `hashtagcms/workflows-java` (also on GHCR as
`ghcr.io/hashtagcms/workflows-java`). Each release is tagged with its exact version and
`latest` — **pin the version in production**. It runs standalone on in-memory H2
out of the box — nothing else to install.

```bash
docker pull hashtagcms/workflows-java:1.0.1
docker run --rm -p 8080:8080 hashtagcms/workflows-java:1.0.1
# then:  curl http://localhost:8080/api/hashtagcms/public/workflows/v1/health
```

**Connect a database.** The default H2 is in-memory (data is lost on restart). The
image bundles the **H2** and **MySQL** drivers; point it at MySQL with env vars:

```bash
# Standalone, Java owns the schema (created + seeded automatically):
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL='jdbc:mysql://db-host:3306/workflows?useSSL=false&allowPublicKeyRetrieval=true' \
  -e SPRING_DATASOURCE_USERNAME=workflows -e SPRING_DATASOURCE_PASSWORD=secret \
  -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver \
  hashtagcms/workflows-java:1.0.1

# Or alongside the PHP app, on the tables PHP owns (validate-only, no seeding):
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=shared \
  -e DB_URL='jdbc:mysql://host.docker.internal:3306/v30?useSSL=false&allowPublicKeyRetrieval=true' \
  -e DB_USERNAME=root -e DB_PASSWORD=secret \
  hashtagcms/workflows-java:1.0.1
```

Or build and run it from source with Compose. It connects to a MySQL you already
run (it doesn't start its own) — copy `.env.example` to `.env`, pick a datasource
option, then:

```bash
docker compose up --build            # H2 with no .env; your running MySQL when .env is set
```

The image is non-root, multi-arch (amd64 + arm64), with a built-in `HEALTHCHECK`
and Spring Boot Actuator — `/actuator/health` (+ `/health/liveness` and
`/health/readiness` for k8s) and `/actuator/prometheus`. For PostgreSQL/MariaDB,
other databases, observability, and every env var, see [Docker](docs/docker.md)
(or embed the [Maven library](#installation) with your own driver).

## Public API

Base path: `/api/hashtagcms/public/workflows/v1` (configurable).

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/execute` | Execute a workflow and return client directives. |
| `GET`  | `/health` | Liveness + registered handlers. |
| `GET`  | `/directives` | The directive capability manifest (filter with `?platform=&app_version=`). |
| `GET`  | `/catalog` | The workflow contract for a site (`?site_id=`): each workflow's alias, expected inputs, and emitted directive types. |

**Execute** request:

```json
{
  "workflow": "WORKFLOW_BUILDER_DEMO",
  "payload": { "code": "SAVE10", "quantity": 1, "page": 1 },
  "site_id": 1,
  "client": { "platform": "web", "app_version": "2.3.0" }
}
```

Response: `{ success, message, directives, data }` — directives use the **flat**
envelope (`{ "type": "toast", "message": "…", "level": "success" }`), and are
**negotiated** to the client (unsupported ones are downgraded to a `fallback` or
dropped per the per-platform manifest).

Try it:

```bash
curl -X POST 'http://localhost:8080/api/hashtagcms/public/workflows/v1/execute' \
  -H 'Content-Type: application/json' \
  -d '{"workflow":"WORKFLOW_LOAD_PHOTOS","payload":{},"client":{"platform":"android","app_version":"2.0.0"}}'
```

## Management API (replaces the admin UI)

| Path | Purpose |
|---|---|
| `/api/hashtagcms/admin/workflows` | CRUD workflows (GET list, GET/{id}, POST, PUT/{id}, DELETE/{id}). |
| `/api/hashtagcms/admin/workflows/preview` | `POST` — dry-run an unsaved `config` through the engine (validation + negotiation), no persistence. Body: `{ config, payload, platform, app_version, site_id, capabilities }`. |
| `/api/hashtagcms/admin/directives` | CRUD the directive manifest. |
| `/api/hashtagcms/admin/logs` | Audit log — `GET` list (newest-first, paginated: `?alias=&page=&size=`), `GET/{id}`, `DELETE/{id}`. |

## Typed clients (server-driven UI)

Compile-time-safe models of the directive contract — a typed directive-type enum
(with each directive's category and negotiation fallback) — are generated from the
manifest for **Kotlin/KMP**, **TypeScript**, and **Swift** under
[`clients/`](clients/). SDUI clients switch over the enum instead of raw strings;
a freshness test keeps them in sync with the server. See [clients/README.md](clients/README.md).

## Concepts

- **Declarative workflows** — a JSON `config` with `validation`, a `target`
  (`http` / `service` / `event` / `none`), and `on_success` / `on_failure`
  branches carrying `message`, `directives`, and an optional `data` object.
  Values interpolate `{{ payload.* }}`, `{{ response.body.* }}`, `{{ user.* }}`,
  `{{ site.id }}`, `{{ env.* }}`, and `{{ x | default: 'y' }}`. `{{ env.KEY }}`
  is the Laravel `env()` equivalent — it resolves through Spring's `Environment`
  (`application.yml`, JVM `-D` system properties, and OS env vars, in Spring's
  usual order), e.g. `{{ env.demo.token }}` or `{{ env.API_TOKEN }}`.
- **Validation** — `PayloadValidator` supports a broad Laravel-style rule set
  (rules piped as `required|string|min:3`, params comma-separated):
  - presence/utility: `required`, `required_if`/`_unless`/`_with`/`_with_all`/`_without`/`_without_all`, `sometimes`, `nullable`, `present`, `filled`, `missing`, `prohibited`, `bail`
  - types: `string`, `integer`, `numeric`, `decimal`, `boolean`, `array`, `list`, `json`, `accepted`, `declined`
  - strings: `email`, `url`, `ip`, `mac_address`, `uuid`, `ulid`, `hex_color`, `alpha`, `alpha_num`, `alpha_dash`, `ascii`, `lowercase`, `uppercase`, `in`, `not_in`, `regex`, `not_regex`, `starts_with`, `ends_with`, `doesnt_start_with`, `doesnt_end_with`, `confirmed`, `same`, `different`
  - numbers/size: `min`, `max`, `size`, `between`, `gt`, `gte`, `lt`, `lte`, `digits`, `digits_between`, `multiple_of`
  - arrays: `distinct`, `in_array`, `contains`, `doesnt_contain`
  - dates: `date`, `date_format`, `after`, `after_or_equal`, `before`, `before_or_equal`, `date_equals`

  (Rules bound to files, a database, the network, or an HTTP request — e.g.
  `exists`, `unique`, `active_url`, `image`, `current_password`, `timezone` — are
  intentionally out of scope for a headless payload validator.)
- **Auth** — the package doesn't authenticate; it reads whoever the host
  authenticated (the analogue of Laravel's `auth()->user()`) via a
  **`WorkflowUserResolver`** bean. The resolved user populates `{{ user.* }}` in
  interpolation and the log's `user_id`, and a workflow marked **`auth_required`**
  is rejected with **401** when no user resolves (`auth.enforce-required`).
  - **Default:** a trusted-gateway resolver that reads forwarded identity headers
    (`X-User-Id` / `X-User-Email` / `X-User-Name`, configurable) — the id header
    marks the request authenticated.
  - **Override:** declare your own `WorkflowUserResolver` bean to integrate Spring
    Security (`SecurityContextHolder`), verify a JWT, or call your IdP.

  ```bash
  # a gateway forwards the verified identity; auth_required workflows then run
  curl -X POST '.../execute' -H 'Content-Type: application/json' \
    -H 'X-User-Id: 14' -H 'X-User-Email: sam@example.com' \
    -d '{"workflow":"WORKFLOW_WHOAMI"}'
  ```
- **Code-based workflows** — implement `WorkflowHandler` and register it with
  `WorkflowHandlerRegistry` by alias.
- **Capability negotiation** — `workflow_directives` is a per-site, per-platform
  manifest (min version + fallback). `DirectiveNegotiator` rewrites the emitted
  directives so a client only receives what it can render; changes are recorded
  on `workflow_logs`.

## Configuration (`application.yml`, prefix `hashtagcms.workflows`)

```yaml
hashtagcms:
  workflows:
    master-site-id: 1
    route-prefix: /api/hashtagcms     # like PHP hashtagcmsapi.route_prefix; public routes = {route-prefix}/public/workflows/v1
    expose-error-details: false       # like PHP expose_error_details
    negotiation:
      enabled: true
    install:
      seed-directives: true
      seed-examples: true
    auth:
      driver: header                  # header (trusted gateway) | sanctum (shared DB) | jwt (external IdP / SSO)
      enforce-required: true          # reject auth_required workflows when no user resolves (401)
      id-header: X-User-Id            # trusted headers used by the `header` driver
      email-header: X-User-Email
      name-header: X-User-Name
```

Switch to MySQL by pointing `spring.datasource.*` at your database, or use the
bundled `shared` profile to run on the PHP app's own database (see
[Shared database + Sanctum](docs/authentication.md#shared-database--sanctum)).
Full property reference: [docs/configuration.md](docs/configuration.md).

## Authentication & SSO

The package does **not** issue credentials — it reads whoever the host has already
authenticated (the Java analogue of Laravel's `auth()->user()`). A single seam,
`WorkflowUserResolver`, turns the incoming request into a user map
(`{id, email?, name?}`); that identity is what `auth_required` checks and what
`{{ user.id }}` / `{{ user.email }}` / `{{ user.name }}` interpolate to. Which
resolver runs is chosen by `hashtagcms.workflows.auth.driver`:

| `auth.driver` | How the user is established | Use when |
|---|---|---|
| `header` (default) | Trusted gateway forwards `X-User-Id` / `X-User-Email` / `X-User-Name` | An API gateway or SSO proxy already authenticated the request upstream |
| `sanctum` | Verify a Laravel Sanctum bearer token against the **shared** `personal_access_tokens` / `users` tables | Running on the same database as the PHP app (see [shared profile](docs/authentication.md#shared-database--sanctum)) |
| `jwt` | Verify an external IdP's JWT (OIDC / SSO) via JWKS or a shared secret | Auth0, Keycloak, Cognito, Okta, Azure AD, Google, or any provider that issues JWTs |

`auth.enforce-required: true` (default) makes an `auth_required` workflow return
**401** when no user resolves; set it to `false` to keep the identity advisory.

- **`jwt`** — verify an external IdP's token (JWKS or shared secret), mapping
  claims to the user; ready-made settings for Auth0, Keycloak, Cognito, Azure AD,
  and Google.
- **`sanctum`** — run on the PHP app's own database and verify its Sanctum tokens.
- **Custom** — declare your own `WorkflowUserResolver` bean (Spring Security, mTLS,
  token introspection, …); it wins via `@ConditionalOnMissingBean`.

See **[docs/authentication.md](docs/authentication.md)** for the full driver
configuration, the provider quick-reference, SSO-to-local account linking, and the
shared-database setup.

## Layout

```
autoconfigure/  WorkflowsAutoConfiguration (the drop-in wiring)
engine/         WorkflowEngine, WorkflowResponse, WorkflowContext,
                VariableInterpolator, PayloadValidator, DirectiveNegotiator, target/*
model/          JPA entities (Workflow, WorkflowDirective, WorkflowLog, User, …) + JSON converters
repository/     Spring Data repositories
security/       WorkflowUserResolver + header/sanctum/jwt drivers
service/        WorkflowService (orchestration + logging), DirectiveManifest, WorkflowCatalog, SeedService
web/            REST controllers (public API + management)
config/         @ConfigurationProperties + startup seeding + env wiring
```

## Tests

```bash
./mvnw test
```

Covers the interpolator, the engine (interpolation + validation + data), the auth
drivers (header + JWT + **Sanctum** token validation), the target adapters
(**http** SSRF guards, **service**, **event**, **custom_class**), the API
end-to-end (health, manifest, per-platform filtering, execute, **catalog**,
**preview**, audit **logs**), Actuator, the **PHP↔Java manifest parity** guard,
the generated-client freshness guard, and the shared-DB schema-compat message.
The suite uses in-memory H2 and needs no external services.

## Building & releasing

```bash
./mvnw clean package        # library jar + runnable -exec jar
./mvnw -Prelease deploy     # signed sources+javadoc, publish to Maven Central
```

The `release` profile attaches sources and Javadoc jars, GPG-signs everything, and
publishes to the Sonatype Central Portal. See [docs/publishing.md](docs/publishing.md)
for the one-time account, namespace, and credential setup.

## License

[MIT](LICENSE) © HashtagCMS. Part of the [HashtagCMS](https://hashtagcms.org)
ecosystem; the reference implementation is the PHP package
[`hashtagcms/workflows`](https://github.com/hashtagcms/workflows).
