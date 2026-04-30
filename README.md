# Room Booking System

A REST API for managing room bookings, built with Kotlin and Spring Boot following hexagonal architecture (ports & adapters).

## Features

- Room management (create, list, get by id)
- User management with role-based access control (USER / ADMIN)
- Booking management with conflict detection and cancellation
- JWT authentication with token blacklisting on logout
- Pessimistic locking to prevent double-bookings under concurrent requests
- OpenAPI documentation via Swagger UI

## Tech stack

- **Kotlin** 2.1 / **Java** 19
- **Spring Boot** 3.4
- **Spring Security** — stateless JWT
- **Spring Data JPA** / **H2** (in-memory)
- **jjwt** 0.12 — JWT generation and validation
- **ArchUnit** 1.3 — architecture rules enforced as tests
- **Maven** multi-module build

## Architecture

Hexagonal architecture with four Maven modules. Dependency flow is strictly one-way:

```
adapter-web ──────┐
                  ├──► domain
adapter-persistence┘
bootstrap ──────────► adapter-web + adapter-persistence
```

| Module | Role |
|---|---|
| `domain` | Pure business logic — no framework dependencies |
| `adapter-web` | REST controllers, DTOs, JWT security stack |
| `adapter-persistence` | JPA entities, Spring Data repositories |
| `bootstrap` | Composition root — wires everything via `AppConfig` |

## Prerequisites

- JDK 19
- Maven 3.9+

## Getting started

```bash
# Clone the repository
git clone https://github.com/koimiyovo/room-booking-system.git
cd room-booking-system

# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run -pl bootstrap
```

The application starts on `http://localhost:8080`.

On startup, `DataInitializer` seeds sample rooms, an admin user, and a regular user.

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
| GET | `/api/users` | ADMIN | List all users |
| GET | `/api/users/{id}` | ADMIN | Get a user by id |
| PUT | `/api/users/{id}` | Authenticated | Update own account |
| DELETE | `/api/users/{id}` | Authenticated | Delete own account |

## Running tests

```bash
# All tests
mvn clean test

# Single module
mvn clean test -pl domain
mvn clean test -pl adapter-web
mvn clean test -pl bootstrap

# Single class (requires prior build of dependencies)
mvn clean test -Dtest=RoomServiceTest
```

The test suite includes domain unit tests, web slice tests (`@WebMvcTest`), integration tests (`@SpringBootTest`), and architecture rules enforced via ArchUnit.

## License

MIT — see [LICENSE](LICENSE).
