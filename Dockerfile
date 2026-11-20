# =============================================================================
# Multi-stage Dockerfile for KEIZOKU Spring Boot Application
# Compatible with Render, Railway, Fly.io, and Docker environments
# =============================================================================

# Stage 1: Build the application JAR with Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Cache dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root application user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built JAR from builder stage
COPY --from=builder /build/target/app.jar app.jar

# Render passes PORT dynamically (default 8080)
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot with container-aware JVM settings
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
