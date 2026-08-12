# ============================================================
# Stage 1: Build the Spring Boot application with Maven
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom first for better layer caching
COPY pom.xml .
COPY src ./src

# Build the jar (skip tests for a faster image build)
RUN mvn -B --no-transfer-progress -DskipTests clean package

# ============================================================
# Stage 2: Minimal runtime image with JRE only
# ============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# JVM memory flags tuned for Render free tier (512 MiB container):
#   -Xmx256m                  cap heap (Serial GC suits small heaps)
#   -XX:MaxMetaspaceSize=160m cap loaded-class memory (biggest consumer)
#   -XX:ReservedCodeCacheSize=64m  cap JIT code cache
#   -XX:MaxDirectMemorySize=64m    cap direct/NIO buffers (PDFBox/Tika)
#   -XX:+ExitOnOutOfMemoryError   exit cleanly on heap OOM so Render restarts
# Expected total footprint: ~350-450 MiB peak, typically 250-350 MiB
ENTRYPOINT ["java", "-Xmx256m", "-XX:MaxMetaspaceSize=160m", "-XX:ReservedCodeCacheSize=64m", "-XX:+UseSerialGC", "-XX:MaxDirectMemorySize=64m", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
