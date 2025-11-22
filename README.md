# ITSEC – Backend Technical Test

This project is a **Spring Boot** backend implementing a simple article library with authentication, MFA, role-based
access control, and audit logging.

Architecture style:

> **Hexagonal (Ports & Adapters) with a Use-Case Centric Application Layer**

The main goals are:

- Easy to read & maintain  
- Clear separation of concerns  
- Easy to plug & play infrastructure (DB, Redis, email, etc.)  
- Easy to scale as the domain grows  
- Easy for other developers to onboard  

---

## Tech Stack

- **Language**: Java 17  
- **Framework**: Spring Boot  
  - Spring Web (REST)  
  - Spring Data JPA (PostgreSQL)  
  - Spring Security  
  - Bean Validation (Jakarta Validation)  
  - Spring Data Redis  
  - Spring Mail  
  - SpringDoc OpenAPI 3 (Swagger UI)  
- **Database**: PostgreSQL  
- **Cache / Rate limiting / OTP**: Redis  
- **Email (dev)**: Mailhog (Docker only)  
- **Build Tool**: Maven  
- **Containerization**: Docker, Docker Compose  

---

## Architecture Overview

The project follows a **Hexagonal (Ports & Adapters)** design with a **Use-Case Centric Application Layer**.

High-level package structure:

```
dev.harscode.itsectest
  domain/         # Pure business models & domain logic
  application/    # Use cases (application services)
  ports/          # Core interfaces (ports) for persistence, cache, email, etc.
  adapters/
    web/          # REST controllers + mappers
    persistence/  # JPA entities + repositories + DB adapters
    cache/        # Redis adapters (OTP store, login attempts, etc.)
    mail/         # Email sender adapter
  web/
    dto/          # Request/response DTOs for the HTTP boundary
  config/         # Spring configuration (security, beans, etc.)
```

---

## Local Development

### 1. Run Without Docker (Recommended for Development)

The project uses:

- `application.yaml` → main configuration  
- `application-local.yaml` → local overrides  

Run with active local profile:

```
-Dspring.profiles.active=local
```

### 2. Running With Docker

Spin up all dependencies:

```
docker compose up -d
```

Docker will load environment variables from `.env`.

---

## API Access

### Base URL

```
http://localhost:8080
```

---

## API Documentation (Swagger UI)

### Swagger UI (frontend)

```
http://localhost:8080/swagger-ui.html
```

or

```
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI JSON (backend schema)

```
http://localhost:8080/v3/api-docs
```

### OpenAPI YAML

```
http://localhost:8080/v3/api-docs.yaml
```

---

## Testing

### Run Unit Tests

```
mvn test
```

### Run Integration Tests

```
mvn verify
```

---

## Running the Application (Local)

```
mvn spring-boot:run
```

or via IntelliJ run the main class:

```
ItsectestApplication
```

---

## Docker Deployment

### Build Image

```
docker build -t itsectest-app .
```

### Start Containers

```
docker compose up -d
```

---

## Security Notes

Spring Boot generates a temporary default password during early development:

- Username: `user`
- Password: printed in startup logs

This will be replaced with custom JWT-based authentication.

---

## License

This project is part of a technical test and is not intended for public distribution.

