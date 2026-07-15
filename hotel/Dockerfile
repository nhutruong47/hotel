# Multi-stage build for the Spring Boot backend.
# Stage 1 - build with Maven + Java 21
# Stage 2 - tiny runtime with JRE only

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first for faster incremental builds.
COPY pom.xml mvnw* ./
COPY .mvn .mvn
RUN ./mvnw -B -q -e -ntp -DskipTests dependency:go-offline || true

COPY src src
RUN ./mvnw -B -e -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as a non-root user.
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /app/target/hotel-backend.jar app.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -q --spider http://localhost:8080/api/v1/health || exit 1

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
