# ============================
# 1. BUILD STAGE
# ============================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom first (dependency caching)
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy sources
COPY src ./src

# Build with Spring Boot layered jar
RUN mvn -q -e -DskipTests package

# ============================
# 2. RUNTIME STAGE
# ============================
FROM eclipse-temurin:21.0.4_7-jre-alpine

WORKDIR /app

# Install timezone (optional but recommended for logging)
RUN apk add --no-cache tzdata

ENV JAVA_OPTS=""
ENV TZ=Asia/Jakarta

# Copy ONLY the final jar
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]