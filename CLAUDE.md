# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build and run all tests
mvn clean test

# Run tests for a single module
mvn clean test -pl domain
mvn clean test -pl infrastructure-api
mvn clean test -pl bootstrap

# Run a single test class
mvn clean test -pl domain -Dtest=RoomServiceTest
mvn clean test -pl infrastructure-api -Dtest=RoomControllerWebMvcTest
mvn clean test -pl bootstrap -Dtest=RoomControllerIntegrationTest

# Start the application (from bootstrap module)
mvn spring-boot:run -pl bootstrap

# Build without tests
mvn clean package -DskipTests
```

**Java version:** 19 (only JDK 19 is installed — do not change `java.version` in pom.xml).

## Architecture

Hexagonal architecture (ports & adapters) with five Maven modules. Dependency flow is strictly one-way:

```
infrastructure-api ──────┐
                         ├──► domain
infrastructure-persistence┘
infrastructure-provider ──► domain
bootstrap ──────────────► infrastructure-api + infrastructure-persistence + infrastructure-provider
```

### Module responsibilities

- **`domain`** — Pure business logic. No framework dependencies. Contains three aggregates and one auth context:
  - `Room` with value objects `RoomId`, `RoomName`, `RoomCapacity` and `NewRoom`. Ports: `RoomUseCase` / `RoomRepository`. `RoomService` depends only on `RoomRepository`.
  - `User` with value objects `UserId`, `UserName`, `UserEmail`, `UserPassword`, `UserRole` and `NewUser` (has `toUser()`), `UpdateUser`. Ports: `UserUseCase` / `UserRepository`. `UserService` depends on `UserRepository` and `PasswordHashPort` (for password re-hashing in `update()`). `UserUseCase` covers read and update operations only — user creation goes through `AuthUseCase`.
  - `Booking` with value objects `BookingId`, `BookingStartDate`, `BookingEndDate`, `BookingNumberOfPeople`, `BookingSpecialRequests` (nullable) and `NewBooking` (has `toBooking()`). Ports: `BookingUseCase` / `BookingRepository`. `BookingService` depends on **both** `BookingRepository` and `RoomRepository` to enforce two business rules: number of people must not exceed room capacity (`RoomCapacityExceededException`), and a room cannot be double-booked on overlapping dates (`BookingConflictException`). `RoomNotFoundException` is thrown when the referenced room does not exist. Domain exceptions live in `domain/exception/`.
  - `BookingService.create` wraps all checks and the save in a single transaction via `TransactionPort` (secondary port, implemented by `SpringTransactionAdapter` in `infrastructure-persistence` using `TransactionTemplate`). The room is fetched with a pessimistic write lock (`RoomRepository.findByIdForBooking`, backed by `SELECT … FOR UPDATE`) to prevent double-booking under concurrent requests.
  - **Authentication context**: `AuthUseCase` / `AuthService` own account creation and login. `AuthService` depends on `UserRepository`, `PasswordHashPort`, and `ClockPort`. `register()` checks email uniqueness, hashes the password, and stamps the registration date via `ClockPort`. `login()` validates credentials (user existence + password match) and returns the domain `User` — it never produces a token (that is an infrastructure concern).
  - **`ClockPort`** — secondary port in `domain/port/secondary/` with a single method `fun now(): OffsetDateTime`. Implemented by `SystemTimeProvider` in `infrastructure-provider`. Used by `AuthService` for deterministic time injection.

- **`infrastructure-api`** — REST layer. Controllers map HTTP to use cases. DTOs live here and are never used in the domain. Also contains the full security stack:
  - `AuthToken` — `@JvmInline value class AuthToken(val value: String)` wrapping the raw JWT string. Used by `JwtService`, `TokenBlacklistService`, `JwtAuthenticationFilter`, `AuthController`, and `LoginResponse`.
  - `JwtService` — generates and validates JWT tokens. `generateToken()` returns `AuthToken`; all other methods accept `AuthToken`.
  - `TokenBlacklistService` — in-memory revocation list using `ConcurrentHashMap<String, Long>`, with a nightly `@Scheduled` cleanup. Public API uses `AuthToken`.
  - `JwtAuthenticationFilter` — extracts the `Bearer` header, wraps it in `AuthToken`, checks the blacklist, validates, and sets the `SecurityContextHolder`.
  - `BCryptPasswordHashAdapter` — implements the domain `PasswordHashPort`. Receives a `PasswordEncoder` via constructor injection (not hardcoded to `BCryptPasswordEncoder`).
  - `SecurityConfig` — stateless JWT security. Declares `@Bean fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()`, which is the single source of truth for the hashing algorithm and is injected into `BCryptPasswordHashAdapter`. Public: `POST /api/auth/register`, `POST /api/auth/login`. ADMIN-only: `GET /api/users`, `GET /api/users/**`, `POST /api/rooms`, `GET /api/bookings`. All other endpoints require authentication.

- **`infrastructure-persistence`** — JPA layer. Persistence adapters implement domain repository ports using Spring Data JPA entities and repositories. H2 in-memory database. Also contains `SpringTransactionAdapter`, which implements `TransactionPort` via `TransactionTemplate`. `InvalidRoleException` lives here (in `adapter.persistence.exception`) — it is a data-integrity concern, not a domain concept, and is thrown by `UserEntity.toDomain()` when an unrecognized role string is found in the database.

- **`infrastructure-provider`** — Time provider. `SystemTimeProvider` implements `ClockPort` using `OffsetDateTime.now()`. Depends only on `domain`. Instantiated in `bootstrap/AppConfig` as a `@Bean`.

- **`bootstrap`** — Composition root. Wires everything via `AppConfig` (`@Bean fun clockPort`, `roomUseCase`, `authUseCase`, `userUseCase`, `bookingUseCase`). Contains `application.yml`, `Application.kt`, `DataInitializer` (seeds sample rooms, users, and bookings on startup via `authUseCase.register()`), and `OpenApiConfig`.

### Key conventions

- Domain objects (`Room`, `RoomId`, etc.) use Kotlin `@JvmInline value class` for type-safe primitives. Jackson serializes them transparently to their underlying types. `AuthToken` in `infrastructure-api` follows the same pattern.
- Services (`RoomService`, `UserService`, `AuthService`, `BookingService`) are not Spring `@Component` — they are instantiated manually in `AppConfig` to keep the domain Spring-free.
- **API field naming**: all JSON fields use `snake_case`. This is enforced by `spring.jackson.property-naming-strategy: SNAKE_CASE` in `application.yml` (and in `infrastructure-api/src/test/resources/application.yml` for the web slice context). Swagger/OpenAPI also reflects snake_case via a `ModelResolver` bean in `OpenApiConfig` that creates a Jackson 2.x `ObjectMapper` with `PropertyNamingStrategies.SNAKE_CASE` — necessary because swagger-core uses Jackson 2.x while Spring Boot 4 uses Jackson 3.x.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) in `infrastructure-api` maps domain exceptions to HTTP status codes:
  - `RoomNotFoundException` → 404
  - `RoomCapacityExceededException` → 400
  - `BookingConflictException` → 409
  - `UserNotFoundException` → 404
  - `EmailAlreadyUsedException` → 409
  - `InvalidCredentialsException` → 401
  - `BookingNotFoundException` → 404
  - `BookingNotOwnedByUserException` → 403
  - `BookingAlreadyCancelledException` → 409
  - `RuntimeException` (catch-all) → 500 with `"Internal server error"` body — catches infrastructure exceptions (e.g. `InvalidRoleException` from `infrastructure-persistence`) that cannot be imported in `infrastructure-api` without violating architecture rules.
- `TransactionPort` is a secondary port in `domain` with a single method `executeInTransaction`. It is implemented by `SpringTransactionAdapter` (`@Component` in `infrastructure-persistence`). Use it to wrap any service operation that requires atomicity across multiple repository calls. The `SpringTransactionAdapter` uses `?: error(...)` instead of `!!` to handle the nullable return of `TransactionTemplate.execute`.
- `RoomRepository.findByIdForBooking` fetches a room with a pessimistic write lock (`SELECT … FOR UPDATE`). Use it — not `findById` — inside `BookingService.create` to prevent concurrent double-bookings for the same room.
- Every controller has its own dedicated DTOs — domain objects never cross the web layer:
  - `XxxResponse` — returned by GET endpoints (`findAll`, `findById`). Has a `companion object { fun fromDomain(domain): XxxResponse }`.
  - `CreateXxxRequest` — received by POST. Has a `fun toNewXxx(): NewXxx` method.
  - `CreateXxxResponse` — returned by POST (201 Created).
- Authentication is split: the domain (`AuthService`) validates credentials and returns a `User`; the web layer (`AuthController` + `JwtService`) generates the `AuthToken`. The domain never handles tokens.
- All OpenAPI responses (`@ApiResponse`) must document every HTTP code the endpoint can actually return, including 401 (authentication required) and 403 (insufficient role).

## Tests

Four test layers, each in its own module:

| Layer        | Classes                                                                                   | Module               | Annotation                                  |
|--------------|-------------------------------------------------------------------------------------------|----------------------|---------------------------------------------|
| Domain unit  | `RoomServiceTest`, `UserServiceTest`, `BookingServiceTest`, `AuthServiceTest`             | `domain`             | none (plain JUnit 5 + mockito-kotlin)       |
| Web slice    | `RoomControllerWebMvcTest`, `UserControllerWebMvcTest`, `BookingControllerWebMvcTest`, `AuthControllerWebMvcTest` | `infrastructure-api` | `@WebMvcTest` |
| Integration  | `RoomControllerIntegrationTest`, `UserControllerIntegrationTest`, `BookingControllerIntegrationTest`, `AuthControllerIntegrationTest` | `bootstrap` | `@SpringBootTest` + `@AutoConfigureMockMvc` |
| Architecture | `ArchitectureTest`                                                                        | `bootstrap`          | plain JUnit 5 + ArchUnit `ClassFileImporter` |

- `@WebMvcTest` requires `TestWebApplication` (in `infrastructure-api/src/test`) as the `@SpringBootApplication` anchor for the web slice.
- Integration tests use `@BeforeEach roomJpaRepository.deleteAll()` for isolation.
- Integration tests are annotated with `@Import(TestTimeProviderConfig::class)` to inject a `MutableTimeProvider` (controllable `ClockPort` implementation defined in `bootstrap/src/test`).
- `mockito-kotlin` (v6.3.0) is used for mocking in domain and web tests.
- `ArchitectureTest` uses `ClassFileImporter().importPackages("com.kyovo")` (not `@AnalyzeClasses`) so that Maven Surefire counts and reports the tests correctly. It enforces: domain free of Spring/JPA, no cross-adapter dependencies, persistence adapter naming convention, no cyclic dependencies within the domain.
- Test names (backtick strings in Kotlin) must be written in English.
- OpenAPI documentation (Swagger annotations: `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, and `OpenApiConfig` descriptions) must be written in English.

### Spring Boot 4 test specifics

- **`@MockBean` / `@SpyBean` are removed** — use `@MockitoBean` / `@MockitoSpyBean` exclusively.
- **`@WebMvcTest` and `@AutoConfigureMockMvc` moved** to a new package: `org.springframework.boot.webmvc.test.autoconfigure` (was `org.springframework.boot.test.autoconfigure.web.servlet`). Always import from this package.
- **Web slice test dependencies** in `infrastructure-api/pom.xml`:
  - `spring-boot-starter-webmvc-test` — provides `@WebMvcTest`, `@AutoConfigureMockMvc`, and MockMvc autoconfiguration.
  - `spring-boot-starter-security-test` — provides `@WithMockUser`, `csrf()`, and Spring Security test support (brings `spring-security-test` transitively).
- `spring-boot-starter-test` is no longer declared explicitly in `infrastructure-api` — it is brought transitively by the two starters above.
