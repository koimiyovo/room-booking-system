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
| `infrastructure-provider` | `SystemTimeProvider` — implements `ClockPort` |
| `bootstrap` | Composition root — wires everything via `AppConfig` |

## Prerequisites

- JDK 19
- Maven 3.9+
- Docker (for PostgreSQL in production and for integration tests)

## Getting started

```bash
# Clone the repository
git clone https://github.com/koimiyovo/room-booking-system.git
cd room-booking-system

# Start a PostgreSQL container
docker run -d --name postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=room_booking -p 5432:5432 postgres:17-alpine
```

**Linux / macOS** — use `make`:

```bash
make build       # build without tests
make run         # start (prod-like, no seed data)
make run-dev     # start with dev profile (wipes and reseeds tables on every startup)
make stop        # stop the application
```

**Windows** — use the PowerShell script:

```powershell
.\scripts.ps1 build
.\scripts.ps1 run
.\scripts.ps1 run-dev
.\scripts.ps1 stop
```

The application starts on `http://localhost:8080`. Flyway applies pending migrations automatically on startup.

`DataInitializer` (seed data) only runs when the `dev` profile is active (`run-dev`).

## API

Interactive documentation is available at `http://localhost:8080/swagger-ui/index.html` once the application is running.

### Authentication

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Create an account |
| POST | `/api/auth/login` | Public | Obtain a JWT token |
| POST | `/api/auth/logout` | Authenticated | Invalidate the current token |

### Rooms

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/rooms` | Authenticated | List all rooms |
| GET | `/api/rooms/{id}` | Authenticated | Get a room by id |
| POST | `/api/rooms` | ADMIN | Create a room |

### Bookings

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/bookings` | ADMIN | List all bookings |
| GET | `/api/bookings/{id}` | Authenticated | Get a booking by id |
| POST | `/api/bookings` | Authenticated | Create a booking |
| POST | `/api/bookings/{id}/cancel` | Authenticated | Cancel a booking |

### Users

| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/users` | ADMIN | List all users (including deleted) |
| GET | `/api/users/{id}` | ADMIN | Get a user by id (404 if deleted) |
| PUT | `/api/users/{id}` | Authenticated | Update own account |
| DELETE | `/api/users/{id}` | Authenticated | Soft-delete own account |
| POST | `/api/users/{id}/validate` | Authenticated | Transition CREATED → ACTIVE |
| POST | `/api/users/{id}/deactivate` | ADMIN | Transition ACTIVE → INACTIVE |
| POST | `/api/users/{id}/reactivate` | ADMIN | Transition INACTIVE → ACTIVE |

## Running tests

**Docker must be running** — integration tests (`bootstrap` module) spin up a real PostgreSQL 17 container via Testcontainers. Domain unit tests and web slice tests have no external dependency.

On Windows, Docker Desktop must have "Expose daemon on TCP without TLS" enabled (Settings → General).

```bash
# All tests (Linux/macOS)
make test

# All tests (Windows)
.\scripts.ps1 test

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
