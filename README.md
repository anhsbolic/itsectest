# ITSEC – Backend Technical Test

This project is a **Spring Boot** backend implementing a simple article library with authentication, MFA, role-based
access control, and audit logging.

Architecture style:

> **Hexagonal (Ports & Adapters) with a use-case centric application layer**

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
- **Database**: PostgreSQL
- **Cache / Rate limiting / OTP**: Redis
- **Email (dev)**: Mailhog (Docker only)
- **Build**: Maven
- **Container**: Docker, Docker Compose

---

## Architecture Overview

The project follows a hexagonal (ports & adapters) design with a use-case centric application layer.

High-level package structure:

```text
com.example.techtest
  domain/        # Pure business models & domain logic
  application/   # Use cases (application services)
  ports/         # Core interfaces (ports) for persistence, cache, email, etc.
  adapters/
    web/         # REST controllers + web mappers
    persistence/ # JPA entities + Spring Data repositories + adapters
    cache/       # Redis adapters (OTP store, login attempts, etc.)
    mail/        # Email sender adapter
  web/
    dto/         # Request/response DTOs for HTTP boundary
  config/        # Spring configuration (security, beans, etc.)