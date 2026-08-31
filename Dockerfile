# syntax=docker/dockerfile:1

# =============================================================================
# HashtagCMS Workflows (Java) — production container image
#
# Multi-stage, multi-arch (linux/amd64 + linux/arm64), non-root, with a built-in
# HEALTHCHECK. Runs standalone out of the box on in-memory H2, or point it at a
# shared MySQL/Postgres via env vars (see `docker-compose.yml` / `.env.example`).
#
# Build locally:   docker build -t hashtagcms/workflows:1.0.0 .
# Run locally:     docker run --rm -p 8080:8080 hashtagcms/workflows:1.0.0
# Multi-arch push: see docker/build-and-push.sh
# =============================================================================

# ---- Build stage: compile the runnable (exec) jar with the Maven Wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies first, for layer caching. A BuildKit cache mount keeps the local
# Maven repo warm across builds so only changed sources are recompiled.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw -B dependency:go-offline || true

# Then the sources.
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B clean package -DskipTests \
 && cp target/*-exec.jar app.jar

# ---- Runtime stage: slim JRE, non-root, healthchecked ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl is used only by the container HEALTHCHECK below.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r app && useradd -r -g app app

COPY --from=build /workspace/app.jar app.jar
USER app

# OCI image metadata (overridable at build time: --build-arg VERSION=...).
ARG VERSION=1.0.0
LABEL org.opencontainers.image.title="HashtagCMS Workflows (Java)" \
      org.opencontainers.image.description="Server-driven workflow & action orchestration engine — Spring Boot, API only." \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.vendor="HashtagCMS" \
      org.opencontainers.image.url="https://hashtagcms.org" \
      org.opencontainers.image.source="https://github.com/hashtagcms/workflows-java" \
      org.opencontainers.image.licenses="MIT"

EXPOSE 8080

# JVM is container-aware; default to a share of the cgroup memory limit. Override
# with JAVA_OPTS, and pass Spring settings as env vars, e.g.
#   -e SPRING_PROFILES_ACTIVE=shared -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=...
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0" \
    SERVER_PORT=8080 \
    HASHTAGCMS_WORKFLOWS_ROUTE_PREFIX=/api/hashtagcms

# Liveness: hit the public health endpoint. Honors the configured route prefix
# and server port, so it keeps working if you override either.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}${HASHTAGCMS_WORKFLOWS_ROUTE_PREFIX}/public/workflows/v1/health" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
