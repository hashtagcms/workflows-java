# syntax=docker/dockerfile:1

# ---- Build stage: compile the runnable (exec) jar with the Maven Wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies first, for layer caching.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline || true

# Then the sources.
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests \
 && cp target/*-exec.jar app.jar

# ---- Runtime stage: slim JRE, non-root ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as an unprivileged user.
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /workspace/app.jar app.jar
USER app

EXPOSE 8080
# Tune the JVM at runtime with JAVA_OPTS, and pass Spring settings as env vars, e.g.
#   -e SPRING_PROFILES_ACTIVE=shared -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=...
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
