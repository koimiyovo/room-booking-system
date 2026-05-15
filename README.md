# Room Booking System

A REST API for managing room bookings, built with Kotlin and Spring Boot following hexagonal architecture (ports & adapters).

## Features

- Room management (create, list, get by id)
- User management with role-based access control (USER / ADMIN)
- User lifecycle: status transitions CREATED → ACTIVE → INACTIVE → DELETED (soft delete)
- Booking management with conflict detection and cancellation
- JWT authentication with token blacklisting on logout
- Deleted users' JWTs are immediately rejected with 401
- Pessimistic locking to prevent double-bookings under concurrent requests
- Email notifications for booking events (created, confirmed, cancelled) and user lifecycle events (registered, deactivated, reactivated, deleted, profile updated)
- OpenAPI documentation via Swagger UI

## Tech stack

- **Kotlin** 2.3 / **Java** 19
- **Spring Boot** 4.0
- **Spring Security** 7 — stateless JWT
- **Spring Data JPA** / **PostgreSQL** (production and integration tests via Testcontainers) — with referential integrity enforced via FK constraints
- **Flyway** 11 — database schema versioning
- **Testcontainers** 2.0 — real PostgreSQL container for integration tests
- **jjwt** 0.13 — JWT generation and validation
- **ArchUnit** 1.4 — architecture rules enforced as tests
- **Maven** multi-module build

## Architecture

Hexagonal architecture with five Maven modules. Dependency flow is strictly one-way:

```
infrastructure-api ──────┐
                         ├──► domain
infrastructure-persistence┘
infrastructure-provider ──► domain
bootstrap ──────────────► infrastructure-api + infrastructure-persistence + infrastructure-provider
```

| Module | Role |
|---|---|
| `domain` | Pure business logic — no framework dependencies |
| `infrastructure-api` | REST controllers, DTOs, JWT security stack |
| `infrastructure-persistence` | JPA entities, Spring Data repositories |
| `infrastructure-provider` | `SystemTimeProvider` (implements `ClockPort`) + `EmailNotificationAdapter` (implements `NotificationPort` via Spring Mail) |
| `bootstrap` | Composition root — wires everything via `AppConfig` |

## Prerequisites

- JDK 19
- Maven 3.9+
- Docker (required for running via Docker Compose and for integration tests)

## Getting started

```bash
git clone https://github.com/koimiyovo/room-booking-system.git
cd room-booking-system
```

### With Docker (recommended)

No local JDK or PostgreSQL needed — everything runs in containers.

**Linux / macOS:**

```bash
make docker-up        # build image and start app + Postgres
make docker-up-dev    # same, with dev profile (seeds sample data)
make docker-down      # stop and remove containers
make docker-down-v    # stop and remove containers + Postgres volume
make docker-logs      # follow application logs
make docker-build     # build the image without starting
```

**Windows:**

```powershell
.\scripts.ps1 docker-up        # build image and start app + Postgres
.\scripts.ps1 docker-up-dev    # same, with dev profile (seeds sample data)
.\scripts.ps1 docker-down      # stop and remove containers
.\scripts.ps1 docker-down-v    # stop and remove containers + Postgres volume
.\scripts.ps1 docker-logs      # follow application logs
.\scripts.ps1 docker-build     # build the image without starting
```

### Without Docker (local Maven)

Requires a running PostgreSQL instance on `localhost:5432` (database `room_booking`, user/password `postgres`).

**Linux / macOS:**

```bash
make build       # package without tests
make run         # start (no seed data)
make run-dev     # start with dev profile (seeds sample data on startup)
make stop        # kill the process on port 8080
make help        # list all available commands
```

**Windows:**

```powershell
.\scripts.ps1 build
.\scripts.ps1 run
.\scripts.ps1 run-dev
.\scripts.ps1 stop
```

The application starts on `http://localhost:8080`. Flyway applies pending migrations automatically on startup.

`DataInitializer` (seed data) only runs when the `dev` profile is active (`run-dev` / `docker-up-dev`).

## Email notifications

The application sends transactional emails on booking events (created, confirmed, cancelled) and user lifecycle events (registered, deactivated, reactivated, deleted, profile updated).

### Local development (Mailpit)

[Mailpit](https://github.com/axllent/mailpit) is included in `docker-compose.yml` as a local SMTP server that catches all outgoing emails without actually sending them.

- SMTP: `localhost:1025`
- Web UI (inbox): `http://localhost:8025`

When running with Docker Compose (`docker-up` / `docker-up-dev`), the app container connects to Mailpit automatically — no extra configuration needed.

When running locally with Maven (`run-dev`), the `dev` Spring profile activates `application-dev.yml`, which points to `localhost:1025`. Start Mailpit first:

```bash
docker compose up mailpit -d
```

### Production

Set the following environment variables:

| Variable | Description |
|---|---|
| `MAIL_HOST` | SMTP server hostname |
| `MAIL_PORT` | SMTP server port |
| `MAIL_FROM` | Sender address (`noreply@...`) |
| `MAIL_USERNAME` | SMTP username (leave empty if not required) |
| `MAIL_PASSWORD` | SMTP password (leave empty if not required) |

The application fails to start if `MAIL_HOST`, `MAIL_PORT`, or `MAIL_FROM` are not set and no active profile provides a default.

## Versioning

The project follows [Semantic Versioning](https://semver.org). The current version is the Maven project version in `pom.xml`.

**Linux / macOS:**

```bash
make release-patch   # bug fix      1.0.0 -> 1.0.1
make release-minor   # new feature  1.0.0 -> 1.1.0
make release-major   # breaking     1.0.0 -> 2.0.0
```

**Windows:**

```powershell
.\scripts.ps1 release-patch
.\scripts.ps1 release-minor
.\scripts.ps1 release-major
```

Each command:
1. Bumps the version in all `pom.xml` files
2. Creates a git commit `chore: release vX.Y.Z`
3. Creates a git tag `vX.Y.Z`
4. Builds the Docker image tagged `room-booking-system:X.Y.Z` and `room-booking-system:latest`

Push to the remote when ready:

```bash
git push && git push --tags
```

## API

All endpoints are prefixed with `/api/v1`. Interactive documentation is available at `http://localhost:8080/swagger-ui/index.html` once the application is running.

### Version

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/v1/version` | Public | Get the application version |

### Authentication

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Create an account |
| POST | `/api/v1/auth/login` | Public | Obtain a JWT token |
| POST | `/api/v1/auth/logout` | Authenticated | Invalidate the current token |

### Rooms

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/v1/rooms` | Authenticated | List all rooms |
| GET | `/api/v1/rooms/{id}` | Authenticated | Get a room by id |
| POST | `/api/v1/rooms` | ADMIN | Create a room |

### Bookings

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/v1/bookings` | ADMIN | List all bookings |
| GET | `/api/v1/bookings/{id}` | Authenticated | Get a booking by id |
| POST | `/api/v1/bookings` | Authenticated | Create a booking |
| POST | `/api/v1/bookings/{id}/cancel` | Authenticated | Cancel a booking |

### Users

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/v1/users` | ADMIN | List all users (including deleted) |
| GET | `/api/v1/users/{id}` | ADMIN | Get a user by id (404 if deleted) |
| PUT | `/api/v1/users/{id}` | Authenticated | Update own account |
| DELETE | `/api/v1/users/{id}` | Authenticated | Soft-delete own account |
| POST | `/api/v1/users/{id}/validate` | Authenticated | Transition CREATED → ACTIVE |
| POST | `/api/v1/users/{id}/deactivate` | ADMIN | Transition ACTIVE → INACTIVE |
| POST | `/api/v1/users/{id}/reactivate` | ADMIN | Transition INACTIVE → ACTIVE |

## CI/CD

The GitHub Actions workflow (`.github/workflows/ci.yml`) runs on every push and pull request to `main`:

1. **Test** — compiles all modules and runs the full test suite (unit, web slice, integration via Testcontainers, architecture).
2. **Publish** *(push to `main` only)* — builds the Docker image and pushes it to the GitHub Container Registry:

```
ghcr.io/koimiyovo/room-booking-system:latest
ghcr.io/koimiyovo/room-booking-system:sha-<commit>
```

Pull the latest image:

```bash
docker pull ghcr.io/koimiyovo/room-booking-system:latest
```

## Running tests

**Docker must be running** — integration tests (`bootstrap` module) spin up a real PostgreSQL 17 container via Testcontainers. Domain unit tests and web slice tests have no external dependency.

On Windows, Docker Desktop must have "Expose daemon on TCP without TLS" enabled (Settings → General).

```bash
# All tests (Linux/macOS)
make test

# All tests (Windows)
.\scripts.ps1 test

# List all available commands
make help
.\scripts.ps1

# Single module
mvn clean test -pl domain
mvn clean test -pl infrastructure-api
mvn clean test -pl bootstrap

# Single class (requires prior build of dependencies)
mvn clean test -Dtest=RoomServiceTest
```

The test suite includes domain unit tests, web slice tests (`@WebMvcTest`), integration tests (`@SpringBootTest` + real PostgreSQL via Testcontainers), and architecture rules enforced via ArchUnit.

## License

MIT — see [LICENSE](LICENSE).
