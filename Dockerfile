# syntax=docker/dockerfile:1

# =============================================================================
# HashtagCMS Workflows (Java) — production container image
#
# Multi-stage, multi-arch (linux/amd64 + linux/arm64), non-root, with a built-in
# HEALTHCHECK. Runs standalone out of the box on in-memory H2, or point it at a
# shared MySQL/Postgres via env vars (see `docker-compose.yml` / `.env.example`).
#
# Build locally:   docker build -t hashtagcms/workflows-java:1.0.1 .
# Run locally:     docker run --rm -p 8080:8080 hashtagcms/workflows-java:1.0.1
# Multi-arch push: see docker/build-and-push.sh
# =============================================================================

# ---- Build stage: compile the jar + build a trimmed jlink runtime ----
# Alpine (musl) JDK so the jlink runtime matches the Alpine runtime base.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Dependencies first, for layer caching. A BuildKit cache mount keeps the local
# Maven repo warm across builds so only changed sources are recompiled.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw -B dependency:go-offline || true

# Then the sources.
COPY src/ src/
COPY docs/ docs/
COPY README.md CHANGELOG.md ./
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B clean package -DskipTests \
 && cp target/*-exec.jar app.jar

# A custom runtime with only the modules Spring Boot + JPA/JDBC + actuator + JWT
# need. The set is conservative (includes jdk.charsets and jdk.localedata so
# non-US charsets/locales keep working, and the crypto modules for TLS + JWT).
RUN "$JAVA_HOME/bin/jlink" \
      --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,jdk.charsets,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.jfr,jdk.localedata,jdk.management,jdk.management.agent,jdk.net,jdk.unsupported \
      --strip-debug --no-header-files --no-man-pages --compress=zip-6 \
      --output /javaruntime

# ---- Runtime stage: bare Alpine + the jlink runtime, non-root, healthchecked ----
FROM alpine:3.24 AS runtime
ENV JAVA_HOME=/opt/java/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"
WORKDIR /app

RUN apk add --no-cache curl \
 && addgroup -S app && adduser -S -G app app

COPY --from=build /javaruntime $JAVA_HOME
COPY --from=build /workspace/app.jar app.jar
USER app

# OCI image metadata (overridable at build time: --build-arg VERSION=...).
ARG VERSION=1.0.1
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

# Health: hit the Actuator health endpoint (aggregates DB connectivity etc.).
# Actuator lives at /actuator regardless of the workflows route prefix.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
