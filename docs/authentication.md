# Authentication & SSO

[← Docs index](README.md)

The package does **not** issue credentials — it reads whoever the host has
already authenticated (the Java analogue of Laravel's `auth()->user()`). A single
seam, [`WorkflowUserResolver`](../src/main/java/org/hashtagcms/workflows/security/WorkflowUserResolver.java),
turns the incoming request into a user map (`{id, email?, name?}`). That identity
is what `auth_required` checks and what `{{ user.id }}` / `{{ user.email }}` /
`{{ user.name }}` interpolate to inside a workflow.

Which resolver runs is chosen by `hashtagcms.workflows.auth.driver`:

| `auth.driver` | How the user is established | Use when |
|---|---|---|
| `header` (default) | Trusted gateway forwards `X-User-Id` / `X-User-Email` / `X-User-Name` | An API gateway or SSO proxy already authenticated the request upstream |
| `sanctum` | Verify a Laravel Sanctum bearer token against the **shared** `personal_access_tokens` / `users` tables | Running on the same database as the PHP app |
| `jwt` | Verify an external IdP's JWT (OIDC / SSO) via JWKS or a shared secret | Auth0, Keycloak, Cognito, Okta, Azure AD, Google, or any JWT issuer |

`auth.enforce-required: true` (default) makes an `auth_required` workflow return
**401** when no user resolves; set it to `false` to keep the identity advisory.

> **Data-driven SSO providers.** The `auth.driver` above is a single, app-wide
> resolver chosen by configuration. For **per-site, per-workflow** external login —
> managed as data (a `workflow_sso_providers` table with `opaque`/`jwt` drivers, a
> configurable credential header, a per-workflow provider pin, and `{{ claims.* }}`
> / `{{ identity.* }}`) — see **[SSO / External-Login Providers](sso-providers.md)**.
> When that module is active it resolves identity per request and falls back to the
> `WorkflowUserResolver` below.

## `header` — trusted gateway (default)

A gateway or SSO proxy authenticates the request and forwards the identity as
headers. The workflow service trusts them. Zero coupling to the login technology.

```yaml
hashtagcms:
  workflows:
    auth:
      driver: header
      id-header: X-User-Id
      email-header: X-User-Email
      name-header: X-User-Name
```

The user counts as authenticated only when the id header is present. Make sure the
gateway strips these headers from inbound client traffic so they cannot be spoofed.

## `jwt` — external SSO / OIDC

Set `auth.driver: jwt` and configure **one** key source — a JWKS endpoint
(asymmetric RS/ES/PS, the usual OIDC case) or a shared `secret` (symmetric HS*).
The service reads `Authorization: Bearer <jwt>`, verifies the signature, checks
`iss` / `aud` / `exp` (with `clock-skew-seconds` tolerance), then maps claims to
the user. Any failure — bad signature, wrong issuer/audience, expiry — resolves to
"no user" (401 for `auth_required`).

```yaml
hashtagcms:
  workflows:
    auth:
      driver: jwt
      jwt:
        jwks-uri: https://YOUR_DOMAIN/.well-known/jwks.json  # or: secret: <shared-secret>
        issuer: https://YOUR_DOMAIN/                          # expected iss (recommended)
        audience: your-api-identifier                         # expected aud (optional)
        id-claim: sub          # claim -> user.id   (default: sub)
        email-claim: email     # claim -> user.email
        name-claim: name       # claim -> user.name
        link-by-email: false   # true = adopt a local users row's numeric id via the email claim
```

### Provider quick-reference

Point `jwks-uri` / `issuer` at your tenant:

| Provider | `jwks-uri` | `issuer` |
|---|---|---|
| **Auth0** | `https://{tenant}.auth0.com/.well-known/jwks.json` | `https://{tenant}.auth0.com/` |
| **Keycloak** | `https://{host}/realms/{realm}/protocol/openid-connect/certs` | `https://{host}/realms/{realm}` |
| **Cognito** | `https://cognito-idp.{region}.amazonaws.com/{poolId}/.well-known/jwks.json` | same base URL |
| **Azure AD** | `https://login.microsoftonline.com/{tenant}/discovery/v2.0/keys` | `https://login.microsoftonline.com/{tenant}/v2.0` |
| **Google** | `https://www.googleapis.com/oauth2/v3/certs` | `https://accounts.google.com` |

Set `audience` to your API identifier where the provider issues it (Auth0 APIs,
Cognito app clients, Azure app registrations).

### Linking SSO identities to local accounts

By default the SSO `sub` becomes `{{ user.id }}` (and `workflow_logs.user_id` is
left null, since `sub` is not a local numeric id). Turn on `link-by-email` to look
the user up in the local `users` table by the email claim and adopt its numeric id,
canonical email, and name — useful when SSO accounts mirror HashtagCMS accounts.

## <a id="shared-database--sanctum"></a>Shared database + Sanctum

This service can run directly on the **same MySQL database as the PHP package** —
reading and writing the same `workflows`, `workflow_directives`, and
`workflow_logs` tables and authenticating against Laravel's own `users` /
`personal_access_tokens`. PHP owns the schema; Java only validates it. Activate the
bundled `shared` profile:

```bash
DB_URL="jdbc:mysql://127.0.0.1:3306/hashtagcms?useSSL=false&allowPublicKeyRetrieval=true" \
DB_USERNAME=root DB_PASSWORD=secret \
./mvnw spring-boot:run -Dspring-boot.run.profiles=shared
```

It sets `ddl-auto: validate` (Hibernate confirms the mappings match the
PHP-created tables and refuses to start on a mismatch), turns seeding off (PHP owns
the data), and selects `auth.driver: sanctum`. An incoming
`Authorization: Bearer {id}|{secret}` is then verified exactly as Laravel Sanctum
does: look up `personal_access_tokens` by id, compare `sha256(secret)` in constant
time, check expiry, load the `users` row. Issue a token from the PHP side with:

```php
$user->createToken('name')->plainTextToken;
```

### Schema expectations

In shared mode the library **never migrates** — PHP owns the schema and Java only
validates it. The database must already contain the tables this version maps:
`workflows`, `workflow_logs`, `workflow_directives`, `users`,
`personal_access_tokens`. Run the PHP package's migrations first, then start the
Java service.

Keep the versions aligned: a given Java library version validates against the
schema shipped by the **matching PHP `hashtagcms/workflows` version** (they move
together — same tables, same columns). If the tables are missing, the DB is wrong,
or the schema has drifted, startup aborts with a clear, actionable message
(see [`SchemaCompatibilityListener`](../src/main/java/org/hashtagcms/workflows/config/SchemaCompatibilityListener.java))
rather than a raw Hibernate stack trace. For a **standalone, Java-owned** database
instead, don't use the shared profile — set `ddl-auto=update` and let the service
create and seed its own schema.

## Custom resolver

Any scheme the built-ins don't cover — Spring Security, mTLS, a bespoke session
store, remote token introspection — is a bean. Declaring your own
`WorkflowUserResolver` makes the built-in bean back off
(`@ConditionalOnMissingBean`), so `auth.driver` is ignored and your resolver runs.

```java
@Bean
WorkflowUserResolver workflowUserResolver() {
    return request -> {
        // authenticate however you like, then return the identity...
        return Map.of("id", 42, "email", "sam@example.com", "name", "Sam");
        // ...or null when there is no user.
    };
}
```

See [Extending](extending.md) for more override points.
