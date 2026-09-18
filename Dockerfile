# =========================
# Stage 1: Build
# =========================
FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy Maven configuration
COPY pom.xml .

# Copy source code
COPY src ./src

# Build Spring Boot application
RUN mvn clean package -DskipTests


# =========================
# Stage 2: Run
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy generated JAR
COPY --from=builder /app/target/tictactoe-1.0.0.jar app.jar

# Render will provide PORT automatically
EXPOSE 8080

# Start application
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]
