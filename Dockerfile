# === BUILD STAGE ===
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom dulu untuk cache dependency
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy source code
COPY src ./src

# Build jar
RUN mvn -q -e -DskipTests package

# === RUNTIME STAGE ===
FROM eclipse-temurin:21-jre-alpine

ENV JAVA_OPTS=""

WORKDIR /app

# Copy jar dari builder
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]