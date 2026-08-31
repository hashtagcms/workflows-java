# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
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

### Fixed
- `WorkflowService.isDeclarative()` now recognizes configs that carry only
  `on_success` / `on_failure` branches (previously mis-routed to the handler
  path).

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
