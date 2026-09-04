# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **SSO / external-login provider module (data-driven), matching the PHP package.**
  A new `workflow_sso_providers` table (per-site with a master-site fallback,
  `alias` unique per site, `driver`, `enabled`, `on_failure`, `cache_ttl`, and a
  `config` JSON) that verifies a client credential and resolves it to a workflow
  identity — no code required:
  - **`opaque` driver** — introspects the token against the login service by
    reusing the `http` target adapter (a `config.verify` request block using
    `{{request.bearer_token}}`, a `config.identity` block mapping
    `{{response.body.*}}` to `{ user_id, claims }`). Verified tokens are cached
    per token-hash for `cache_ttl` seconds; failures are never cached.
  - **`jwt` driver** — verifies the signature locally against the provider's
    `jwks_url` or shared `secret`, enforces `issuer`/`audience`, and maps
    `{{token.*}}` to the identity. No per-request introspection call.
  - **Configurable credential source** — the token is read from
    `Authorization: Bearer` by default, or from any header via a `credential`
    block (`{ "header": "authToken", "strip_prefix": "Bearer " }`); a configured
    header is authoritative (no fallback).
  - **Per-workflow provider pin** — `workflows.sso_provider_alias` pins a specific
    provider; the resolver honours it and falls back to the site default
    (deterministic: site-over-master, then id) when a pin no longer resolves. The
    reserved value `@none` ignores SSO entirely (local login only).
  - **`on_failure` policy** — `reject` surfaces a rejected credential as an HTTP
    **401**; `anonymous` runs the workflow unauthenticated. When no provider is
    configured for a site, resolution falls back to the host's
    `WorkflowUserResolver` (local guard), so non-SSO installs are unaffected.
  - **Identity in the engine** — external subjects log to
    `workflow_logs.external_user_id` + `sso_provider_alias`, and the provider's
    claims are available to interpolation as `{{ claims.* }}` and `{{ identity.* }}`
    (`user_id`, `external_user_id`, `provider`, `raw`).
  - **Admin REST CRUD** — `GET/POST/PUT/DELETE {route-prefix}/admin/sso-providers`
    (the API-only replacement for the PHP admin UI), with per-site alias
    validation. The `shared`-mode schema guard now also validates
    `workflow_sso_providers`. See [docs/sso-providers.md](docs/sso-providers.md).
  - **Explicit-identity execution** — `WorkflowService.execute(..., WorkflowIdentity)`
    lets a requestless caller (queue job / server-to-server) pass an identity that
    wins over resolution, mirroring PHP's `Workflows::execute(..., identity:)`.
    `WorkflowIdentity.from(...)` coerces a numeric id / user map / `WorkflowIdentity`.
  - **Companion login workflow** — the opt-in `WORKFLOW_LOGIN_TEST` seed (a workflow
    that *obtains* a token from an external login API), enabled via
    `hashtagcms.workflows.install.seed-login-test` + `install.login-test-url`.
- **`X-Site-Id` request header** — the execute API now resolves the site from the
  body `site_id`, else the `X-Site-Id` header, else the master site (PHP parity).
- **Landing page + in-app docs at `/`** — instead of Spring Boot's Whitelabel Error
  Page, `/` serves a small signpost page (live endpoints with the configured
  `route-prefix` substituted in, links to the guides) and `/docs.html` renders the
  bundled Markdown docs client-side. Gated by `hashtagcms.workflows.docs.enabled`
  (**off by default** so the library never claims a consumer app's `/` route; **on**
  in the standalone runner / Docker image). Assets live under
  `classpath:/workflows-site/` — never `static/` — so nothing is auto-served to
  library consumers.

### Fixed
- **Blocked (401) runs are now written to `workflow_logs` (as unsuccessful)** —
  a rejected credential or an unauthenticated `auth_required` call previously threw
  before logging, leaving no audit record; it is now logged before the 401 is
  surfaced, matching the PHP reference. The `auth_required` message is now
  `"Authentication required."` (was a longer Java-specific string).
- **Preview (dry-run) API** — `POST {route-prefix}/admin/workflows/preview` runs
  an unsaved `config` through the engine (validation + directive negotiation)
  with no persistence.
- **Workflow catalog API** — `GET {route-prefix}/public/workflows/v1/catalog`
  returns each workflow's alias, expected inputs, and emitted directive types.
- **Audit-log read API** — `GET/DELETE {route-prefix}/admin/logs` (newest-first,
  paginated, filterable by `alias`).
- **Docker packaging** — multi-stage, non-root, multi-arch (amd64 + arm64) image
  with a built-in `HEALTHCHECK`; `docker-compose.yml` that runs on H2 by default
  and connects to your own running MySQL via `.env` (standalone or `shared`);
  `.env.example`; and a multi-arch `docker/build-and-push.sh`. See
  [docs/docker.md](docs/docker.md).
- **PHP↔Java parity guard** — `DirectiveManifestParityTest` asserts the Java
  directive manifest matches the reference PHP implementation. The fixture
  `src/test/resources/php-directive-manifest.json` is regenerated from PHP by
  `scripts/dump-php-manifest.php`; the test fails and names any directive whose
  type / category / per-platform support / fallback (or label / description) drifts.
- **Spring Boot Actuator** (optional dependency) — health with Kubernetes
  liveness/readiness probes (`/actuator/health`, `/actuator/health/liveness`,
  `/actuator/health/readiness`) and Prometheus metrics (`/actuator/prometheus`).
  The Docker `HEALTHCHECK` now targets `/actuator/health` (DB-aware). Exposure is
  configured under `management.*`; as a library it activates only when
  `spring-boot-starter-actuator` is on the consumer's classpath.
- **Compatibility matrix** ([docs/compatibility.md](docs/compatibility.md)) —
  documents how the library, PHP reference, HTTP API (`v1`), config schema (`1.0`),
  directive manifest, and client `app_version` negotiation relate and version.
- **Shared-DB schema-compatibility guard** — when the app runs with
  `ddl-auto=validate` (the `shared` profile) against a database missing/mismatching
  the PHP-owned workflow tables, startup now fails with a clear, actionable message
  (`SchemaCompatibilityListener`) instead of a raw Hibernate stack trace, and a
  positive "schema validated OK" line is logged on success. Documented the
  Java-lib ↔ PHP-schema version expectations in the shared-database guide.
- **Typed client models (SDUI codegen)** — a directive-type enum (with each
  directive's category and negotiation fallback) generated from the manifest for
  Kotlin/KMP, TypeScript, and Swift under `clients/`, so server-driven-UI clients
  handle directives with compile-time safety. `ClientModelGeneratorTest` regenerates
  (`-Dcodegen.write=true`) and otherwise guards that the committed files stay in sync.
- **HTTP target hardening (SSRF)** — the `http` target adapter now enforces an
  optional egress allowlist (`hashtagcms.workflows.http.allowed-hosts`) and an
  optional private-network block (`block-private-networks`, covering
  loopback/private/link-local and `169.254.169.254`), uses one shared
  connection-pooling client with redirects disabled (so they can't bypass the
  allowlist), and retries transient failures (IOException / 429 / 5xx) for
  idempotent requests with linear backoff. Supports a per-target `idempotencyKey`.
  Defaults preserve current behavior (no allowlist, no retries).

### Changed
- **Smaller Docker image (~574 MB → ~257 MB)** — the runtime now uses a trimmed
  `jlink` custom Java runtime on bare Alpine instead of the full Ubuntu JRE.
  Still multi-arch (amd64 + arm64), non-root, healthchecked. The module set lives
  in the `Dockerfile` (`--add-modules`); extend it if a new dependency needs a
  module not listed.

### Fixed
- `WorkflowService.isDeclarative()` now recognizes configs that carry only
  `on_success` / `on_failure` branches (previously mis-routed to the handler
  path).
- H2 web console is now off by default (`H2_CONSOLE_ENABLED`, and hard-off in the
  `shared` profile) so it is never exposed with a real database.
- Replaced the deprecated `UriComponentsBuilder.fromHttpUrl` with `fromUriString`
  in the HTTP target adapter.
- Aligned the `progress` directive description with the PHP reference (en-dash in
  `0–100`), caught by the new parity guard.

### Tests
- Added end-to-end coverage for the catalog, preview (dry-run + non-persistence),
  and audit-log read/delete endpoints (+10 tests).
- Added unit tests for `SanctumTokenUserResolver` (token validation, wrong secret,
  expiry, malformed headers) and the `service` / `event` / `custom_class` target
  adapters (+11 tests).

## [1.0.0] — 2026-08-29

First public release. A Spring Boot auto-configuration library that ports the
HashtagCMS server-driven workflow engine to Java (API only).

### Added
- **Workflow engine** — declarative config (`validation` → `target` →
  `on_success` / `on_failure` → `directives` + `data`), `{{ }}` interpolation
  with the `| default:` filter, and whole-token real-value semantics.
- **Targets** — `http`, `service`, `event`, and `custom_class` / `handler`
  adapters.
- **72-directive manifest** with per-site / per-platform capability negotiation
  (SemVer min-version, fallback chains, `capabilities` override, telemetry).
- **Payload validation** — a broad, Laravel-style rule set (~60 rules).
- **Public + management REST API** under a configurable route prefix
  (`hashtagcms.workflows.route-prefix`).
- **Authentication** via a pluggable `WorkflowUserResolver`, with built-in
  drivers: `header` (trusted gateway), `sanctum` (shared Laravel tables), and
  `jwt` (external IdP / SSO — JWKS or shared secret). `auth_required`
  enforcement is configurable.
- **Shared-database mode** — run on the PHP package's own MySQL schema
  (`ddl-auto: validate`) via the bundled `shared` profile.
- **Spring Boot auto-configuration** — drop the dependency in and the whole
  engine wires itself; every bean is overridable (`@ConditionalOnMissingBean`).
- Env interpolation (`{{ env.KEY }}`) resolved through Spring's `Environment`.

[Unreleased]: https://github.com/hashtagcms/workflows-java/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/hashtagcms/workflows-java/releases/tag/v1.0.0
