# =============================================================================
# Multi-stage Dockerfile for KEIZOKU Spring Boot Application
# Compatible with Render, Railway, Fly.io, and Docker environments
# =============================================================================

# Stage 1: Build the application JAR with Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build production JAR (matches finalName 'app' in pom.xml)
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root application user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built JAR from builder stage
COPY --from=builder /build/target/app.jar app.jar

# Render passes PORT dynamically (defaults to 8080 if unset)
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot with container-aware JVM settings and shell expansion for PORT
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar app.jar"]
