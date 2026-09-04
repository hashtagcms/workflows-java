# Getting started

[← Docs index](README.md)

## 1. Add the dependency

**Maven**

```xml
<dependency>
  <groupId>org.hashtagcms</groupId>
  <artifactId>workflows</artifactId>
  <version>1.0.0</version>
</dependency>
```

**Gradle**

```groovy
implementation 'org.hashtagcms:workflows:1.0.0'
```

You also need a JDBC driver on the classpath (the library ships none — it's the
host's choice), e.g. `com.h2database:h2` for a quick start or
`com.mysql:mysql-connector-j` for MySQL, plus a `spring.datasource.*`
configuration if you're not using an embedded database.

## 2. Nothing else to wire

The library is a **Spring Boot auto-configuration**. Adding it to the classpath
registers, against your existing `DataSource`:

- the workflow **execution engine** and directive **negotiator**;
- the public + management **REST controllers**;
- the **directive manifest** and example **seeding** (toggle in config);
- the **auth resolver** (default: trusted headers).

You do **not** add `@Import`, `@ComponentScan`, `@EntityScan`, or
`@EnableJpaRepositories`. The auto-configuration registers the library's package
additively, so your own entities and repositories keep being scanned as usual.

If you want to run it as a service instead of embedding it, see
[Run standalone](../README.md#run-standalone).

## 3. Execute a workflow

The public API lives under `/api/hashtagcms/public/workflows/v1` (the prefix is
[configurable](configuration.md)).

```bash
curl -X POST 'http://localhost:8080/api/hashtagcms/public/workflows/v1/execute' \
  -H 'Content-Type: application/json' \
  -d '{
        "workflow": "WORKFLOW_LOAD_PHOTOS",
        "payload": {},
        "client": { "platform": "android", "app_version": "2.0.0" }
      }'
```

Response — the **flat** directive envelope, negotiated to the client:

```json
{
  "success": true,
  "message": "Loaded 30 photos",
  "directives": [
    { "type": "toast", "level": "success", "message": "Loaded 30 photos" }
  ],
  "data": { "photos": [ /* ... */ ] }
}
```

## 4. Define your own workflow

Workflows are rows in the `workflows` table; create them over the management API
(`POST /api/hashtagcms/admin/workflows`) or seed them yourself. A workflow's
`config` is declarative JSON:

```json
{
  "version": "1.0",
  "validation": { "email": "required|email" },
  "target": {
    "type": "http",
    "method": "POST",
    "url": "https://api.example.com/subscribe",
    "headers": { "Accept": "application/json" },
    "body": { "email": "{{ payload.email }}" }
  },
  "on_success": {
    "message": "Subscribed {{ payload.email }}",
    "data": { "id": "{{ response.body.id }}" },
    "directives": [
      { "type": "toast", "level": "success", "message": "You're in!" }
    ]
  },
  "on_failure": {
    "directives": [
      { "type": "toast", "level": "error", "message": "Could not subscribe" }
    ]
  }
}
```

Key ideas:

- **`{{ }}` interpolation** reads from `payload`, `response`, `user`, and `env`;
  `{{ token | default: 'x' }}` supplies a fallback, and a whole-token expression
  returns the real (typed) value, not a stringified one.
- **`target.type`** is `http`, `service`, `event`, `custom_class` (a Java
  handler), or `none` (skip straight to the branches).
- **Directives** use the flat envelope (`{ "type": ..., ...fields }`) and are
  negotiated per client platform / app version before returning.

## 5. Try, inspect, and audit — over REST

Because there's no admin UI, the same things the PHP admin screens did are exposed
as REST endpoints:

- **Preview (dry-run)** — run an unsaved `config` through the full engine
  (validation + negotiation) without persisting anything, to check it before you
  save:

  ```bash
  curl -X POST 'http://localhost:8080/api/hashtagcms/admin/workflows/preview' \
    -H 'Content-Type: application/json' \
    -d '{"config":{ /* the JSON above */ },"payload":{"email":"a@b.com"},"platform":"web"}'
  ```

- **Catalog** — the machine-readable contract for a site (each workflow's alias,
  expected inputs, and emitted directive types), so a client can validate calls
  and catch drift: `GET /api/hashtagcms/public/workflows/v1/catalog?site_id=1`.
- **Logs** — every execution is logged; read them newest-first (optionally per
  alias): `GET /api/hashtagcms/admin/logs?alias=WORKFLOW_LOAD_PHOTOS&page=0&size=25`,
  `GET /api/hashtagcms/admin/logs/{id}`.

## Landing page

Running the standalone app (jar or Docker) and opening `/` in a browser shows a
small landing page — the live endpoints (with your configured `route-prefix`) and
links to these guides, rendered in-app at `/docs.html`. It replaces Spring Boot's
Whitelabel Error Page. It is **off by default** and only served when
`hashtagcms.workflows.docs.enabled=true` (the standalone/Docker config sets it), so
embedding this library in another Spring Boot app never claims that app's `/` route.

See [Configuration](configuration.md) for the knobs, [Authentication & SSO](authentication.md)
for wiring identity, and [Extending](extending.md) for Java handlers, custom
targets, and directives.
