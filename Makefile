.PHONY: help run run-dev stop build test \
        docker-build docker-up docker-up-dev docker-down docker-down-v docker-logs

help:
	@echo ""
	@echo "Maven:"
	@echo "  run            Start the application"
	@echo "  run-dev        Start with dev profile (seeds sample data)"
	@echo "  stop           Kill the process on port 8080"
	@echo "  build          Package without tests"
	@echo "  test           Run all tests"
	@echo ""
	@echo "Docker:"
	@echo "  docker-build   Build the Docker image"
	@echo "  docker-up      Build and start all containers"
	@echo "  docker-up-dev  Build and start with dev profile (seeds sample data)"
	@echo "  docker-down    Stop and remove containers"
	@echo "  docker-down-v  Stop and remove containers + Postgres volume"
	@echo "  docker-logs    Follow application logs"
	@echo ""

# ── Maven ────────────────────────────────────────────────────────────────────

run:
	mvn spring-boot:run -pl bootstrap

run-dev:
	mvn spring-boot:run -pl bootstrap -Dspring.profiles.active=dev

stop:
	@-pkill -f "spring-boot" 2>/dev/null || true

build:
	mvn clean package -DskipTests

test:
	mvn clean test

# ── Docker ───────────────────────────────────────────────────────────────────

docker-build:
	docker compose build

docker-up:
	docker compose up --build

docker-up-dev:
	SPRING_PROFILES_ACTIVE=dev docker compose up --build

docker-down:
	docker compose down

docker-down-v:
	docker compose down -v

docker-logs:
	docker compose logs -f app
