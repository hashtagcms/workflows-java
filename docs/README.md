# Documentation

Guides for **HashtagCMS Workflows (Java)** — the Spring Boot auto-configuration
library. Start with the project [README](../README.md) for the overview and
install snippet, then dive in here.

| Guide | What it covers |
|---|---|
| [Getting started](getting-started.md) | Add the dependency, execute your first workflow, define workflows. |
| [Configuration](configuration.md) | Every `hashtagcms.workflows.*` property, profiles, datasource. |
| [Docker](docker.md) | Run the published image, Compose profiles (H2 / MySQL), env vars, multi-arch publishing. |
| [Authentication & SSO](authentication.md) | The resolver seam and the `header` / `sanctum` / `jwt` drivers. |
| [Extending](extending.md) | Custom handlers, targets, directives, and a custom user resolver. |
| [Publishing](publishing.md) | Releasing the library to Maven Central. |

## How it fits together

The library is server-driven: a workflow is a declarative JSON `config`
(validation → target → `on_success`/`on_failure` branches carrying a `message`,
`directives`, and `data`). The engine runs it, interpolates `{{ }}` tokens,
negotiates the resulting directives down to what the calling client can render,
logs the execution, and returns `{ success, message, directives, data }`. The
client renders the directives. Nothing is rendered server-side.

This Java library is a faithful port of the PHP package
[`hashtagcms/workflows`](https://github.com/hashtagcms/workflows); the two share
the same concepts, config shape, directive manifest, and — optionally — the same
database.
