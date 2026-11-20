# ============================================================
# Dockerfile for Nhu Villas Backend
# Multi-stage build for smaller image
# ============================================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Add non-root user for security
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

WORKDIR /app

# Create logs directory
RUN mkdir -p logs

# Copy jar from builder
COPY --from=builder /app/target/hotel-backend.jar ./app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# 8080 = application; 8081 = actuator when MANAGEMENT_SERVER_PORT=8081 (internal only)
EXPOSE 8080 8081

# Health check (actuator port follows MANAGEMENT_SERVER_PORT, default = app port)
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget -q --spider "http://localhost:${MANAGEMENT_SERVER_PORT:-8080}/actuator/health" || exit 1

# Default JVM tuning keeps the existing 512MB Render deployment working.
# docker-compose.yml overrides JAVA_OPTS (heap from the container limit:
# -XX:MaxRAMPercentage=75) for the VPS deployment.
ENV JAVA_OPTS="-XX:+UseContainerSupport -Xmx220m -Xms96m -XX:MaxMetaspaceSize=170m -XX:CompressedClassSpaceSize=32m -XX:ReservedCodeCacheSize=48m -Xss256k -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
