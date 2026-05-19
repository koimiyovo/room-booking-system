# Room Booking System

A room booking application built with hexagonal architecture (ports & adapters) on both the backend (Kotlin / Spring Boot REST API) and the frontend (React / TypeScript SPA).

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

### Backend
- **Kotlin** 2.3 / **Java** 19
- **Spring Boot** 4.0
- **Spring Security** 7 — stateless JWT
- **Spring Data JPA** / **PostgreSQL** (production and integration tests via Testcontainers) — with referential integrity enforced via FK constraints
- **Flyway** 11 — database schema versioning
- **Testcontainers** 2.0 — real PostgreSQL container for integration tests
- **jjwt** 0.13 — JWT generation and validation
- **ArchUnit** 1.4 — architecture rules enforced as tests
- **Maven** multi-module build

### Frontend
- **React** 19 / **TypeScript** 6 — Vite 8 dev server
- **React Router** 7 — client-side routing with protected routes
- **TanStack Query** 5 — server state management
- **shadcn/ui** + **Tailwind CSS** 4 — component library and utility CSS
- **Vitest** 4 + **Testing Library** + **MSW** 2 — unit, hook, and UI tests

## Architecture

Both layers follow hexagonal architecture (ports & adapters). Domain logic has zero framework dependencies; adapters live at the edges.

### Backend — five Maven modules

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

### Frontend — `frontend/` (React SPA)

```
ui/ (React components, hooks, pages)
  └──► domain/ (services, use cases)
         └──► infrastructure/ (API adapters, token storage)
```

| Layer | Role |
|---|---|
| `domain/model/` | TypeScript interfaces — `Room`, `Booking`, `User` |
| `domain/port/primary/` | Use case interfaces — `RoomUseCase`, `BookingUseCase`, `AuthUseCase` |
| `domain/port/secondary/` | Repository interfaces implemented by adapters |
| `domain/service/` | Pure TypeScript services — delegate to repository ports |
| `infrastructure/api/` | HTTP adapters (fetch + MSW-testable), snake_case → camelCase mapping |
| `infrastructure/auth/` | `TokenStorage` — JWT in localStorage |
| `ui/context/` | `AuthContext` — current user + token (Context API) |
| `ui/hooks/` | TanStack Query hooks — bridge between use cases and React |
| `ui/pages/` | Login, Register, Rooms, Bookings, Users (admin) |
| `ui/components/` | Layout (Navbar, PrivateRoute, AdminRoute) + domain components |

## Prerequisites

- JDK 19
- Maven 3.9+
- Node.js 20+ / npm
- Docker (required for running via Docker Compose and for integration tests)

## Getting started

```bash
git clone https://github.com/koimiyovo/room-booking-system.git
cd room-booking-system
```

### Environment variables

Copy `.env.example` to `.env` and fill in the values before running anything:

```bash
cp .env.example .env
```

| Variable | Description |
|---|---|
| `POSTGRES_USER` | PostgreSQL username |
| `POSTGRES_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Base64-encoded secret key for signing JWT tokens |
| `MAIL_HOST` | SMTP server hostname |
| `MAIL_PORT` | SMTP server port |
| `MAIL_FROM` | Sender address for outgoing emails |

Docker Compose reads `.env` automatically. For local Maven runs, export the variables in your shell or use a tool like `direnv`.

### With Docker — full stack (recommended)

Builds and starts everything in containers: PostgreSQL, Mailpit, Spring Boot backend, and the React frontend served by Nginx.

| URL | Service |
|---|---|
| `http://localhost` | Frontend (Nginx → React SPA) |
| `http://localhost/api/v1/...` | API (proxied by Nginx → Spring Boot) |
| `http://localhost:8080` | Backend direct access |
| `http://localhost:8025` | Mailpit inbox |

**Linux / macOS:**

```bash
make docker-up        # build and start all containers (frontend + backend + db + mailpit)
make docker-up-dev    # same, with dev profile (seeds sample data)
make docker-down      # stop and remove containers
make docker-down-v    # stop and remove containers + Postgres volume
make docker-logs      # follow application logs
make docker-build     # build all images without starting
```

**Windows:**

```powershell
.\scripts.ps1 docker-up        # build and start all containers
.\scripts.ps1 docker-up-dev    # same, with dev profile (seeds sample data)
.\scripts.ps1 docker-down      # stop and remove containers
.\scripts.ps1 docker-down-v    # stop and remove containers + Postgres volume
.\scripts.ps1 docker-logs      # follow application logs
.\scripts.ps1 docker-build     # build all images without starting
```

### Without Docker — local development

Runs backend and frontend in separate processes. No containers needed (except optionally for PostgreSQL and Mailpit).

**Linux / macOS:**

```bash
make dev         # start backend (dev profile) + frontend together — Ctrl+C stops both
make run         # backend only (no seed data)
make run-dev     # backend only with dev profile
make fe-dev      # frontend only (http://localhost:5173)
make stop        # kill the process on port 8080
make help        # list all available commands
```

**Windows:**

```powershell
.\scripts.ps1 dev        # backend in a new window + frontend here
.\scripts.ps1 run
.\scripts.ps1 run-dev
.\scripts.ps1 fe-dev
.\scripts.ps1 stop
```

In local dev mode, the Vite dev server runs on `http://localhost:5173` and proxies `/api` to `http://localhost:8080`. Flyway applies pending migrations automatically on startup.

`DataInitializer` (seed data) only runs when the `dev` profile is active (`run-dev` / `make dev` / `docker-up-dev`).

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

Set `MAIL_HOST`, `MAIL_PORT`, and `MAIL_FROM` via environment variables (see the [Environment variables](#environment-variables) section). The application fails to start if any of these are missing. `MAIL_USERNAME` and `MAIL_PASSWORD` default to empty — only set them if your SMTP server requires authentication.

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

### Backend

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

### Frontend

No external dependencies — all tests run in-process with Vitest + jsdom + MSW.

```bash
cd frontend

npm test           # run all tests once
npm run test:watch # watch mode
npm run test:ui    # Vitest UI (browser)
```

The frontend test suite covers:
- **Domain unit tests** — pure TypeScript, no framework (`RoomService`, `BookingService`, `AuthService`)
- **Adapter tests** — HTTP mapping with MSW mocking the API (`RoomApiAdapter`)
- **Hook tests** — TanStack Query hooks with MSW (`useRooms`)
- **Page tests** — React Testing Library + MSW (`LoginPage`)

## License

MIT — see [LICENSE](LICENSE).
