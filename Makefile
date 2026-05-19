.PHONY: help run run-dev stop build test \
        fe-dev fe-test fe-build dev \
        docker-build docker-up docker-up-dev docker-down docker-down-v docker-logs \
        release-patch release-minor release-major _release

VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null)

help:
	@echo ""
	@echo "Maven (backend):"
	@echo "  run            Start the application"
	@echo "  run-dev        Start with dev profile (seeds sample data)"
	@echo "  stop           Kill the process on port 8080"
	@echo "  build          Package without tests"
	@echo "  test           Run all tests"
	@echo ""
	@echo "Frontend:"
	@echo "  fe-dev         Start the Vite dev server (http://localhost:5173)"
	@echo "  fe-test        Run frontend tests"
	@echo "  fe-build       Build the frontend for production"
	@echo ""
	@echo "Full stack:"
	@echo "  dev            Start backend (dev profile) + frontend together (Ctrl+C stops both)"
	@echo ""
	@echo "Docker:"
	@echo "  docker-build   Build the Docker image"
	@echo "  docker-up      Build and start all containers"
	@echo "  docker-up-dev  Build and start with dev profile (seeds sample data)"
	@echo "  docker-down    Stop and remove containers"
	@echo "  docker-down-v  Stop and remove containers + Postgres volume"
	@echo "  docker-logs    Follow application logs"
	@echo ""
	@echo "Release (current: $(VERSION)):"
	@echo "  release-patch  bump patch  e.g. $(VERSION) -> x.y.Z+1  (bug fix)"
	@echo "  release-minor  bump minor  e.g. $(VERSION) -> x.Y+1.0  (new feature)"
	@echo "  release-major  bump major  e.g. $(VERSION) -> X+1.0.0  (breaking change)"
	@echo ""

# -- Maven -------------------------------------------------------------------

run:
	mvn install -DskipTests && mvn spring-boot:run -pl bootstrap

run-dev:
	mvn install -DskipTests && mvn spring-boot:run -pl bootstrap -Dspring.profiles.active=dev

stop:
	@-pkill -f "spring-boot" 2>/dev/null || true

build:
	mvn clean package -DskipTests

test:
	mvn clean test

# -- Frontend ----------------------------------------------------------------

fe-dev:
	cd frontend && npm run dev

fe-test:
	cd frontend && npm test

fe-build:
	cd frontend && npm run build

# -- Full stack --------------------------------------------------------------

dev:
	@trap 'kill 0' INT; \
	(mvn install -DskipTests && mvn spring-boot:run -pl bootstrap -Dspring.profiles.active=dev) & \
	(cd frontend && npm run dev) & \
	wait

# -- Docker ------------------------------------------------------------------

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

# -- Release -----------------------------------------------------------------

release-patch:
	$(MAKE) _release NEW_VERSION=$(shell echo "$(VERSION)" | awk -F. '{print $$1"."$$2"."$$3+1}')

release-minor:
	$(MAKE) _release NEW_VERSION=$(shell echo "$(VERSION)" | awk -F. '{print $$1"."$$2+1".0"}')

release-major:
	$(MAKE) _release NEW_VERSION=$(shell echo "$(VERSION)" | awk -F. '{print $$1+1".0.0"}')

_release:
	@echo "Releasing v$(NEW_VERSION)..."
	mvn versions:set -DnewVersion=$(NEW_VERSION) -DgenerateBackupPoms=false
	git add pom.xml domain/pom.xml infrastructure-api/pom.xml infrastructure-persistence/pom.xml infrastructure-provider/pom.xml bootstrap/pom.xml
	git commit -m "chore: release v$(NEW_VERSION)"
	git tag v$(NEW_VERSION)
	APP_VERSION=$(NEW_VERSION) docker compose build
	docker tag room-booking-system:$(NEW_VERSION) room-booking-system:latest
	@echo ""
	@echo "Released v$(NEW_VERSION) — push when ready: git push && git push --tags"
