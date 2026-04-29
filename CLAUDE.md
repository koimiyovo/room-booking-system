# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build and run all tests
mvn clean test

# Run tests for a single module
mvn clean test -pl domain
mvn clean test -pl adapter-web
mvn clean test -pl bootstrap

# Run a single test class
mvn clean test -pl domain -Dtest=RoomServiceTest
mvn clean test -pl adapter-web -Dtest=RoomControllerWebMvcTest
mvn clean test -pl bootstrap -Dtest=RoomControllerIntegrationTest

# Start the application (from bootstrap module)
mvn spring-boot:run -pl bootstrap

# Build without tests
mvn clean package -DskipTests
```

**Java version:** 19 (only JDK 19 is installed — do not change `java.version` in pom.xml).

## Architecture

Hexagonal architecture (ports & adapters) with four Maven modules. Dependency flow is strictly one-way:

```
adapter-web ──────┐
                  ├──► domain
adapter-persistence┘
bootstrap ──────────► adapter-web + adapter-persistence
```

### Module responsibilities

- **`domain`** — Pure business logic. No framework dependencies. Contains three aggregates:
  - `Room` with value objects `RoomId`, `RoomName`, `RoomCapacity` and `NewRoom`. Ports: `RoomUseCase` / `RoomRepository`. `RoomService` depends only on `RoomRepository`.
  - `User` with value objects `UserId`, `UserName`, `UserEmail` and `NewUser` (has `toUser()`). Ports: `UserUseCase` / `UserRepository`. `UserService` depends only on `UserRepository`.
  - `Booking` with value objects `BookingId`, `BookingStartDate`, `BookingEndDate`, `BookingNumberOfPeople`, `BookingSpecialRequests` (nullable) and `NewBooking` (has `toBooking()`). Ports: `BookingUseCase` / `BookingRepository`. `BookingService` depends on **both** `BookingRepository` and `RoomRepository` to enforce two business rules: number of people must not exceed room capacity (`RoomCapacityExceededException`), and a room cannot be double-booked on overlapping dates (`BookingConflictException`). `RoomNotFoundException` is thrown when the referenced room does not exist. Domain exceptions live in `domain/exception/`.

- **`adapter-web`** — REST layer. Controllers map HTTP to use cases. DTOs live here and are never used in the domain.

- **`adapter-persistence`** — JPA layer. `RoomPersistenceAdapter` implements `RoomRepository` using
  `RoomJpaRepository` (Spring Data JPA) and `RoomEntity`. H2 in-memory database.

- **`bootstrap`** — Composition root. Wires everything via `AppConfig` (`@Bean fun roomUseCase`, `userUseCase`, `bookingUseCase`). Contains `application.yml`, `Application.kt`, and `DataInitializer` (seeds sample rooms, users, and bookings on startup).

### Key conventions

- Domain objects (`Room`, `RoomId`, etc.) use Kotlin `@JvmInline value class` for type-safe primitives. Jackson
  serializes them transparently to their underlying types.
- Services (`RoomService`, `UserService`, `BookingService`) are not Spring `@Component` — they are instantiated manually in `AppConfig` to keep the domain Spring-free.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) in `adapter-web` maps domain exceptions to HTTP status codes: `RoomNotFoundException` → 404, `RoomCapacityExceededException` → 400, `BookingConflictException` → 409.
- Every controller has its own dedicated DTOs — domain objects never cross the web layer:
  - `XxxResponse` — returned by GET endpoints (`findAll`, `findById`). Has a `companion object { fun fromDomain(domain): XxxResponse }`.
  - `CreateXxxRequest` — received by POST. Has a `fun toNewXxx(): NewXxx` method.
  - `CreateXxxResponse` — returned by POST (201 Created).

## Tests

Three test layers, each in its own module:

| Layer       | Classes                                                                 | Module        | Annotation                                  |
|-------------|-------------------------------------------------------------------------|---------------|---------------------------------------------|
| Domain unit | `RoomServiceTest`, `UserServiceTest`, `BookingServiceTest`              | `domain`      | none (plain JUnit 5 + mockito-kotlin)       |
| Web slice   | `RoomControllerWebMvcTest`, `UserControllerWebMvcTest`, `BookingControllerWebMvcTest` | `adapter-web` | `@WebMvcTest`            |
| Integration | `RoomControllerIntegrationTest`, `UserControllerIntegrationTest`, `BookingControllerIntegrationTest` | `bootstrap` | `@SpringBootTest` + `@AutoConfigureMockMvc` |

- `@WebMvcTest` requires `TestWebApplication` (in `adapter-web/src/test`) as the `@SpringBootApplication` anchor for the
  web slice.
- Integration tests use `@BeforeEach roomJpaRepository.deleteAll()` for isolation.
- `mockito-kotlin` (v5.4.0) is used for mocking in domain and web tests.
- Test names (backtick strings in Kotlin) must be written in English.
- OpenAPI documentation (Swagger annotations: `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, and `OpenApiConfig` descriptions) must be written in English.
