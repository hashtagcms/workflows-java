# Configuration

[← Docs index](README.md)

All settings live under the `hashtagcms.workflows.*` prefix and bind to
[`WorkflowProperties`](../src/main/java/org/hashtagcms/workflows/config/WorkflowProperties.java).
Because the library ships `spring-boot-configuration-processor`, your IDE
auto-completes and documents these keys.

## Full reference

```yaml
hashtagcms:
  workflows:
    master-site-id: 1              # fallback site id when no site-specific row exists
    route-prefix: /api/hashtagcms  # base for all routes (see below)
    expose-error-details: false    # include raw exception messages in API errors (keep false in prod)

    negotiation:
      enabled: true                # downgrade/drop directives per the client's platform + app version

    install:
      seed-directives: true        # seed the 72-directive manifest on startup
      seed-examples: true          # seed the example workflows on startup

    auth:
      driver: header               # header | sanctum | jwt   (see Authentication & SSO)
      enforce-required: true       # reject auth_required workflows with 401 when no user resolves
      id-header: X-User-Id         # trusted headers used by the `header` driver
      email-header: X-User-Email
      name-header: X-User-Name
      jwt:                         # used only when driver: jwt
        jwks-uri:                  # https://YOUR_DOMAIN/.well-known/jwks.json  (RS/ES/PS)
        secret:                    # shared secret for HS* (use instead of jwks-uri)
        algorithm:                 # override; default RS256 (jwks) or HS256 (secret)
        issuer:                    # expected iss claim
        audience:                  # expected aud claim
        clock-skew-seconds: 60
        id-claim: sub
        email-claim: email
        name-claim: name
        link-by-email: false       # map the SSO email to a local users row for a numeric id
```

## Routes

Everything hangs off `route-prefix` (default `/api/hashtagcms`):

| Prefix | Routes |
|---|---|
| `{route-prefix}/public/workflows/v1` | `POST /execute`, `GET /health`, `GET /directives`, `GET /catalog` |
| `{route-prefix}/admin/workflows` | CRUD workflows, `POST /preview` (dry-run) |
| `{route-prefix}/admin/directives` | CRUD the directive manifest |
| `{route-prefix}/admin/logs` | Read/prune the audit log (`GET` list, `GET/{id}`, `DELETE/{id}`) |

This mirrors the PHP package's `hashtagcmsapi.route_prefix`, so a Java service and
a PHP service can present identical URLs.

## Datasource

The library uses whatever `DataSource` your application configures — it does not
define one. Standard Spring Boot properties apply:

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/mydb
    username: app
    password: secret
  jpa:
    hibernate:
      ddl-auto: update   # update for a Java-owned schema; validate when sharing PHP's
```

The bundled **`shared`** profile points at the PHP package's own MySQL database
with `ddl-auto: validate` and `auth.driver: sanctum` — see
[Authentication & SSO → Shared database](authentication.md#shared-database--sanctum).

## Overriding beans

Every bean the library defines is `@ConditionalOnMissingBean`. Declare your own
bean of the same type and it wins — no configuration flag needed. The most common
case is the user resolver; see [Extending](extending.md).
