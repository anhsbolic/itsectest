SHELL := /bin/bash

# ==== CONFIG ====
APP_NAME ?= itsectest
MVN ?= ./mvnw
DC ?= docker compose

# ==== PHONY TARGETS ====
.PHONY: help run dev test clean package \
        docker-build docker-up docker-down docker-logs docker-restart \
        fmt env

# ==== HELP ====
help:
	@echo ""
	@echo "Usage:"
	@echo "  make run           - Run Spring Boot locally (no Docker)"
	@echo "  make dev           - Run app with Docker Compose (db + redis + mailhog + app)"
	@echo "  make test          - Run unit tests"
	@echo "  make clean         - Clean Maven build"
	@echo "  make package       - Build jar (without tests)"
	@echo "  make docker-build  - Build Docker image only"
	@echo "  make docker-up     - Start Docker Compose (app + deps)"
	@echo "  make docker-down   - Stop Docker Compose"
	@echo "  make docker-logs   - Tail logs from Docker app"
	@echo "  make docker-restart- Restart app container"
	@echo "  make env           - Copy .env.example -> .env (if not exists)"
	@echo ""

# ==== LOCAL JAVA RUN ====
run:
	$(MVN) spring-boot:run

dev: docker-up

test:
	$(MVN) test

clean:
	$(MVN) clean

package:
	$(MVN) -DskipTests package

fmt:
	@echo "No formatter configured yet. Add Spotless/Checkstyle if needed."

# ==== ENV ====
env:
	@if [ -f ".env" ]; then \
	  echo ".env already exists, skip copy"; \
	else \
	  cp .env.example .env && echo "Created .env from .env.example"; \
	fi

# ==== DOCKER / COMPOSE ====
docker-build:
	$(DC) build

docker-up:
	$(DC) up --build -d

docker-down:
	$(DC) down

docker-logs:
	$(DC) logs -f app

docker-restart:
	$(DC) restart app
	$(DC) logs -f app