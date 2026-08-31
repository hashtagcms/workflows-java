# Docker

[← Docs index](README.md)

The library also ships as a self-contained container image, so you can run the
whole engine as a microservice with no JDK, Maven, or database to install. It runs
standalone on in-memory **H2** out of the box, and can be pointed at **MySQL** with
environment variables.

- **Base:** `eclipse-temurin:21-jre` (Ubuntu), **non-root** (`app` user).
- **Multi-arch:** `linux/amd64` + `linux/arm64`.
- **Port:** `8080`.
- **Health:** built-in `HEALTHCHECK` against the public health endpoint.
- **Bundled JDBC drivers:** **H2** and **MySQL** (see [Database](#database)).

## Where to pull the image

| Registry | Image | Pull |
|---|---|---|
| **Docker Hub** (primary) | `hashtagcms/workflows` | `docker pull hashtagcms/workflows:1.0.0` |
| **GitHub Container Registry** | `ghcr.io/hashtagcms/workflows` | `docker pull ghcr.io/hashtagcms/workflows:1.0.0` |

**Tags:** each release is published as its exact version (e.g. `1.0.0`) and as
`latest`. **Pin the exact version in production**; use `latest` only for quick
trials. The version tag always matches the Maven artifact version and the Git tag.

## Pull & run

```bash
docker pull hashtagcms/workflows:1.0.0
docker run --rm -p 8080:8080 hashtagcms/workflows:1.0.0
```

Verify it:

```bash
curl http://localhost:8080/api/hashtagcms/public/workflows/v1/health
curl 'http://localhost:8080/api/hashtagcms/public/workflows/v1/catalog?site_id=1'
```

That default run uses an **in-memory H2** database — everything works immediately,
but all data is lost when the container stops. For anything real, connect a
database (next section).

## Database

The service needs a JDBC datasource. What it should connect to depends on your
scenario:

| Scenario | Database | How | Persists? |
|---|---|---|---|
| **Quick trial / demo** | In-memory **H2** (default) | Nothing to configure | ❌ lost on restart |
| **Standalone, own data** | **MySQL** (Java owns the schema) | `SPRING_DATASOURCE_*` env vars, `ddl-auto=update` | ✅ |
| **Alongside the PHP app** | **MySQL** (PHP owns the schema) | `SPRING_PROFILES_ACTIVE=shared` + `DB_*` env vars | ✅ (shared) |

> The image bundles the **H2** and **MySQL** JDBC drivers. To use **PostgreSQL**,
> **MariaDB**, or another engine in a container, either embed the library in your
> own Spring Boot app with that driver on the classpath (see the
> [Maven install](#use-the-maven-library-instead) below) or rebuild this image
> with the driver added — the driver is deliberately the host's choice.

### 1. Standalone against your own MySQL (Java-owned schema)

Java creates and owns the tables (`ddl-auto=update`) and seeds the manifest +
examples. Override the datasource directly with standard Spring env vars:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL='jdbc:mysql://db-host:3306/workflows?useSSL=false&allowPublicKeyRetrieval=true' \
  -e SPRING_DATASOURCE_USERNAME=workflows \
  -e SPRING_DATASOURCE_PASSWORD=secret \
  -e SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
  hashtagcms/workflows:1.0.0
```

### 2. Alongside the PHP app (shared, PHP-owned schema)

Java operates on the **same tables** PHP owns, so it only *validates* the schema
(`ddl-auto=validate`), skips seeding, and authenticates via Sanctum tokens. Turn
this on with the `shared` profile and the `DB_*` vars:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=shared \
  -e DB_URL='jdbc:mysql://db-host:3306/v30?useSSL=false&allowPublicKeyRetrieval=true' \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=secret \
  hashtagcms/workflows:1.0.0
```

Reaching a database on the **host** from inside the container: use
`host.docker.internal` (Docker Desktop on macOS/Windows) or the host IP /
`--network=host` on Linux.

## Docker Compose

Compose does **not** start its own database — it connects to a MySQL you already
run. With no `.env` it runs on in-memory H2:

```bash
docker compose up --build          # in-memory H2 (self-contained)
```

To use your **running MySQL**, copy `.env.example` to `.env`, uncomment one of the
two options in it, then `docker compose up`. `.env` is gitignored, and the service
loads it automatically (and maps `host.docker.internal` so it can reach a MySQL
container that publishes 3306 on the host).

```bash
cp .env.example .env               # edit: pick Option A (own schema) or B (shared)
docker compose up --build
```

- **Option A** — standalone against your MySQL, Java owns the schema
  (`SPRING_DATASOURCE_*`, `ddl-auto=update`).
- **Option B** — shared with the PHP app on an existing HashtagCMS database
  (`SPRING_PROFILES_ACTIVE=shared` + `DB_*`, validate-only, no seeding).

Reach a MySQL container that publishes 3306 on the host with
`host.docker.internal:3306` (works on macOS/Windows/Linux here); or, if the
database is on the same Docker network, use its container name and add that
network to the `workflows` service.

## All environment variables

Everything is driven by env vars — Spring relaxed binding maps `SPRING_...` and
`HASHTAGCMS_WORKFLOWS_...` onto the properties in
[configuration](configuration.md).

| Variable | Default | Purpose |
|---|---|---|
| `JAVA_OPTS` | `-XX:MaxRAMPercentage=75.0` | JVM flags. The JVM is container-aware and sizes the heap from the cgroup memory limit. |
| `SERVER_PORT` | `8080` | HTTP port (also used by the healthcheck). |
| `SPRING_PROFILES_ACTIVE` | *(none)* | `shared` to run against an existing HashtagCMS DB (validate-only, no seeding). |
| `SPRING_DATASOURCE_URL` | *(H2 in-memory)* | JDBC URL of your database. |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `sa` / *(empty)* | Database credentials. |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `org.h2.Driver` | `com.mysql.cj.jdbc.Driver` for MySQL. |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | `update` for a Java-owned schema; `validate` when sharing PHP's. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | *(none)* | Consulted **only** by the `shared` profile. |
| `HASHTAGCMS_WORKFLOWS_ROUTE_PREFIX` | `/api/hashtagcms` | Base API path (also used by the healthcheck). |
| `HASHTAGCMS_WORKFLOWS_EXPOSE_ERROR_DETAILS` | `false` | Return real exception messages in API errors (dev only). |

## Build the image yourself

```bash
docker build -t hashtagcms/workflows:1.0.0 .
docker run --rm -p 8080:8080 hashtagcms/workflows:1.0.0
```

The build is a two-stage Dockerfile: stage one compiles the runnable `-exec.jar`
with the Maven Wrapper (a BuildKit cache mount keeps `~/.m2` warm between builds),
stage two copies just that jar onto a slim JRE.

## Use the Maven library instead

If you already have a Spring Boot app, you usually don't need the image at all —
add the library and bring your own JDBC driver:

```xml
<dependency>
  <groupId>org.hashtagcms</groupId>
  <artifactId>workflows</artifactId>
  <version>1.0.0</version>
</dependency>
<!-- your database driver, e.g. PostgreSQL -->
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <scope>runtime</scope>
</dependency>
```

Then configure `spring.datasource.*` as usual — the library uses whatever
`DataSource` your app defines and supports any database Hibernate does. See
[Getting started](getting-started.md) and [Configuration](configuration.md).

## Publishing multi-arch images

`docker/build-and-push.sh` builds and pushes `linux/amd64` + `linux/arm64` with
`buildx`, tagging both the exact version (read from `pom.xml`) and `latest`:

```bash
docker login                                # or: docker login ghcr.io
./docker/build-and-push.sh                  # hashtagcms/workflows:<pom-version> + :latest
VERSION=1.2.0 ./docker/build-and-push.sh    # a specific version
IMAGE=ghcr.io/hashtagcms/workflows ./docker/build-and-push.sh
PUSH=false ./docker/build-and-push.sh       # local single-arch build, no push
```

Keep the image version in lockstep with the Maven `pom.xml` `<version>` and the
Git tag so the jar, the artifact, and the image all agree.
