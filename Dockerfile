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

ENTRYPOINT ["java", "-jar", "app.jar"]
