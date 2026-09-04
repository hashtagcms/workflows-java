# SSO / External-Login Providers

The **SSO provider module** resolves *who* is executing a workflow from an
external login service — verify a client credential, map it to a workflow
identity — with **no code**, driven entirely by rows in the
`workflow_sso_providers` table. It is the data-driven counterpart to the
config-driven [`WorkflowUserResolver`](authentication.md): when the SSO module is
active (at least one enabled, published provider), it resolves identity per
request (and per workflow); otherwise the local `WorkflowUserResolver` is used, so
non-SSO installs are unaffected.

This mirrors the PHP reference package feature-for-feature. The tables are shared
and PHP-owned; in `shared` mode the Java library validates them.

## The two drivers

A provider row has a `driver`:

- **`opaque`** — the token is opaque to us (e.g. an encrypted JWE), so it must be
  **introspected** by calling the login service. The `config.verify` block is an
  HTTP request formatter (reusing the `http` target adapter) and `config.identity`
  maps the response:

  ```json
  {
    "verify": {
      "url": "https://login.example.com/user/profile",
      "method": "GET",
      "headers": { "Authorization": "Bearer {{request.bearer_token}}" }
    },
    "identity": {
      "user_id": "{{response.body.userId}}",
      "claims": { "email": "{{response.body.email}}" },
      "raw": "{{response.body}}"
    }
  }
  ```

  Verified tokens are cached per token-hash for `cache_ttl` seconds; failures are
  never cached.

- **`jwt`** — the token is a signed JWT verified **locally** against the provider's
  `jwks_url` (asymmetric) or `secret` (symmetric), enforcing `issuer`/`audience`.
  Claims are mapped via `{{ token.* }}`:

  ```json
  {
    "jwks_url": "https://idp.example.com/.well-known/jwks.json",
    "issuer": "https://idp.example.com",
    "audience": "workflows",
    "identity": {
      "user_id": "{{token.sub}}",
      "claims": { "email": "{{token.email}}" }
    }
  }
  ```

  > The `jwt` driver verifies signatures with `com.nimbusds:nimbus-jose-jwt`, which
  > ships with the library — no extra dependency to add (the PHP reference uses
  > `firebase/php-jwt`). The `opaque` driver needs nothing beyond the HTTP client
  > already used by the `http` target.

## Where the token comes from (`credential`)

By default the token is read from `Authorization: Bearer <token>`. Many APIs use a
**different header**, sometimes with a prefix — point at it with a `credential`
block (works for both drivers):

```json
"credential": { "header": "authToken", "strip_prefix": "Bearer " }
```

When `credential.header` is set it is **authoritative** — the driver does *not*
fall back to `Authorization`, and a request without that header is treated as
anonymous. The extracted token becomes `{{request.bearer_token}}` for the verify
block; incoming headers are also forwardable via `{{request.headers.*}}` (names
are lower-cased).

### Interpolation sources

The `opaque` `verify` and `identity` blocks (and the `jwt` `identity` block) may
reference:

| Source | Where | Meaning |
| --- | --- | --- |
| `{{request.bearer_token}}` | verify | The extracted credential (see above). |
| `{{request.headers.*}}` | verify | Incoming request headers, names lower-cased (e.g. `{{request.headers.x-api-key}}`). |
| `{{request.query.*}}` | verify | Incoming query-string parameters. |
| `{{response.body.*}}` | identity (`opaque`) | The parsed verify response body. |
| `{{response.status}}` | identity (`opaque`) | The verify response's HTTP status. |
| `{{response.headers.*}}` | identity (`opaque`) | The verify response headers. |
| `{{token.*}}` | identity (`jwt`) | The verified JWT's claims. |

## Which provider a workflow uses

Identity is resolved by the **site's default provider** — the site's own
enabled+published row over the master-site fallback, then the lowest id
(deterministic). The site is taken from the request body's `site_id`, else the
`X-Site-Id` header, else the configured master site. To use a *specific* provider
for a workflow, set its `sso_provider_alias`:

- a **provider alias** — that provider is used (falling back to the site default
  if the alias no longer resolves);
- **`@none`** — SSO is ignored for that workflow; identity comes from the local
  guard only (useful for a provider created ahead of use);
- **null/empty** — the site default.

## Using the identity inside a workflow

- `{{ claims.* }}` — the normalized identity attributes a provider maps (e.g.
  `{{ claims.email }}`).
- `{{ identity.* }}` — `user_id`, `external_user_id`, `provider`, and `raw`
  (`{{ identity.raw.* }}` is the opt-in verbatim passthrough when the provider
  maps a `raw` key).

## Enforcement (401)

`WorkflowService.execute` blocks before the handler when the credential was
**rejected** (a bad/expired token under an `on_failure = reject` provider —
`"Invalid or expired credentials."`, always a 401), or when the workflow is
`auth_required` and no identity resolved (`"Authentication required."`). Under
`on_failure = anonymous`, a rejected credential runs the workflow unauthenticated
instead. The blocked run throws `UnauthorizedException`, which the execution
controller maps to HTTP **401** with an error toast — and the attempt is **still
written to `workflow_logs` (as unsuccessful)**, so blocked calls leave an audit
trail, matching the PHP reference. (`auth_required` enforcement can be turned off
with `hashtagcms.workflows.auth.enforce-required=false`.)

### `auth_required` and the validation request (no black box)

`auth_required` is only the *switch*: the engine will not run the workflow unless
a valid identity resolved. It does **not** say *how* the token is checked — that is
the provider's job, and nothing is hidden. With an `opaque` provider whose `verify`
block is:

```json
"verify": {
  "url": "https://login.example.com/user/v1/me",
  "method": "GET",
  "headers": { "Accept": "application/json", "Authorization": "Bearer {{request.bearer_token}}" }
}
```

the engine issues exactly that request (the caller's token substituted for
`{{request.bearer_token}}`) before running the workflow:

| Validation request result | What the engine does |
| --- | --- |
| **2xx** | Maps the response into the identity (`user_id` + `claims`) and runs the workflow. |
| **non-2xx** (e.g. 401) | Identity is *rejected*; with `on_failure: reject` the run returns **401** `"Invalid or expired credentials."` and the workflow never executes. |
| **no token sent** | With `auth_required: true`, returns **401** `"Authentication required."` — the validation request is not even made. |

A `jwt` provider makes **no** outbound call: it verifies the token's signature
locally against the cached JWKS (or shared secret) instead.

## Requestless callers (jobs / server-to-server)

A queued or server-to-server caller has no HTTP request to resolve a token from.
Such a caller passes an identity straight into `execute` — it wins over resolution
(the Java analogue of PHP's `Workflows::execute(..., identity:)`):

```java
service.execute(alias, payload, siteId, platform, appVersion, capabilities,
        /* user */ null, /* request */ null,
        WorkflowIdentity.from(userIdOrMapOrIdentity));
```

`WorkflowIdentity.from(...)` accepts a numeric id, a `{id,email,name}` map, an
existing `WorkflowIdentity`, or `null` (anonymous).

## Companion: a login workflow (obtaining a token)

SSO providers *verify* a token; a workflow can *obtain* one. The opt-in
`WORKFLOW_LOGIN_TEST` seed POSTs `{{payload.email}}` / `{{payload.password}}` to an
external HashtagCMS login API and returns the issued token + user via
`on_success.data` — credentials are never stored in the workflow. It is **not**
seeded by default (it targets a specific host); enable it and point it at your
environment:

```yaml
hashtagcms:
  workflows:
    install:
      seed-login-test: true
      login-test-url: "https://auth.example.com/api/hashtagcms/public/user/v1/login"
```

The client then stores the returned token and sends it as `Authorization: Bearer …`
on later calls, which an SSO provider verifies.

## What gets logged

Each run records the resolved caller in `workflow_logs`: `user_id` (local numeric
id) **or** `external_user_id` + `sso_provider_alias` (external subject), plus the
usual payload/directives/timing. Blocked (401) attempts are logged too, as
unsuccessful.

## Managing providers (admin REST API)

The API-only replacement for the PHP admin UI, under
`{route-prefix}/admin/sso-providers`:

| Method & path | Action |
| --- | --- |
| `GET /admin/sso-providers` | List providers |
| `GET /admin/sso-providers/{id}` | Get one |
| `POST /admin/sso-providers` | Create (validates `alias` per site, `driver`, `on_failure`) |
| `PUT /admin/sso-providers/{id}` | Update |
| `DELETE /admin/sso-providers/{id}` | Delete |

`alias` is unique **per site**, matches `^[A-Za-z0-9._-]+$`; `driver` is
`opaque`/`jwt`; `on_failure` is `reject`/`anonymous`.
