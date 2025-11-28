# ------------------------------
# Stage 1: Build
# ------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gradleの場合 (Mavenの場合は mvnw と pom.xml をコピー)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# ビルド実行 (テストはスキップして高速化)
RUN ./gradlew bootJar -x test

# ------------------------------
# Stage 2: Run
# ------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# ビルド成果物をコピー
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]