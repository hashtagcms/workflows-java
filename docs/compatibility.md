# Compatibility matrix

The moving parts version independently. This is what each version means, its
current value, and what a bump implies.

| Axis | Current | What it is | Bumped when |
|---|---|---|---|
| **Java library** | `org.hashtagcms:workflows` **1.0.1** | The Maven artifact / Docker image tag. | Any release of this project (SemVer). |
| **PHP reference** | `hashtagcms/workflows` (matching release) | The reference implementation this port tracks; both share the DB schema + directive manifest. | Released in lockstep with the Java library (see [parity guard](../src/test/java/org/hashtagcms/workflows/DirectiveManifestParityTest.java)). |
| **HTTP API** | **v1** (`/public/workflows/v1`) | The public REST contract (paths + request/response envelope). | Only on a breaking API change → a new `/v2` path (v1 kept). |
| **Config schema** | **1.0** (`config.version`) | The declarative workflow-config shape (`validation` / `target` / `on_success` / `on_failure` / `directives` / `data`). Advisory today — the engine does not branch on it; it marks the shape for forward compatibility. | On a breaking config-shape change. |
| **Directive manifest** | **72 directives** (ships with lib 1.0.1) | The catalogue of directive types + per-platform support + fallbacks. Not separately numbered — it moves with the library version. | Whenever directives are added/changed (a library release). |

## Java ↔ PHP

The Java library is a functional port of the PHP package and they are released
**together** with the same directive manifest and the same database schema. Run a
Java library version against the **matching** PHP schema. In `shared` mode Java
never migrates — it validates against PHP-owned tables and refuses to start on a
mismatch (see [Authentication → Schema expectations](authentication.md#schema-expectations)).
Drift is caught both ways: `DirectiveManifestParityTest` (Java side) and
`php artisan workflows:check-java-parity` (PHP side).

## Client app versions (directive negotiation)

Clients do **not** need to match the server version. Every `/execute` call may send
`client.platform` + `client.app_version`; the server negotiates each emitted
directive down to what that client can render:

- A directive with **no per-platform minimum** (most of them) is supported on every
  platform and every app version.
- A directive gated for a platform (e.g. `haptic` on `android`/`ios` from `1.0`) is
  sent only when the client's `app_version` is **≥ that minimum** (SemVer compare).
  Otherwise it is replaced by its `fallback` (e.g. `toast`) or dropped.
- Omit `app_version` and all directives are considered supported (no gating).

So a **newer server stays compatible with older clients** (they transparently get
fallbacks), and **older servers stay compatible with newer clients** (newer clients
render everything the server emits). Fetch the exact contract for a client with
`GET /public/workflows/v1/directives?platform=&app_version=` (what that client can
render) and `GET /public/workflows/v1/catalog?site_id=` (each workflow's inputs and
the directive types it emits).

## Versioning policy

- **Library** follows SemVer. A patch/minor never breaks the v1 API or the 1.0
  config schema; directives are added, not removed, within a major.
- **API** is versioned in the path; a breaking change introduces `/v2` and keeps
  `/v1`.
- **Directives** are additive within a major version; a client that doesn't know a
  new type simply won't have it gated to it (or receives its fallback).
