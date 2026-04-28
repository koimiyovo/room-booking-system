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

- **`domain`** — Pure business logic. No framework dependencies. Contains the `Room` aggregate, value objects (`RoomId`,
  `RoomName`, `RoomCapacity`), `NewRoom`, and two port interfaces: `RoomUseCase` (primary/inbound) and
  `RoomRepository` (secondary/outbound). `RoomService` implements `RoomUseCase` and depends only on `RoomRepository`.

- **`adapter-web`** — REST layer. `RoomController` maps HTTP to `RoomUseCase`. DTOs (`CreateRoomRequest`,
  `CreateRoomResponse`) live here and are never used in the domain.

- **`adapter-persistence`** — JPA layer. `RoomPersistenceAdapter` implements `RoomRepository` using
  `RoomJpaRepository` (Spring Data JPA) and `RoomEntity`. H2 in-memory database.

- **`bootstrap`** — Composition root. Wires everything via `AppConfig` (`@Bean fun roomUseCase`). Contains
  `application.yml` and `Application.kt`.

### Key conventions

- Domain objects (`Room`, `RoomId`, etc.) use Kotlin `@JvmInline value class` for type-safe primitives. Jackson
  serializes them transparently to their underlying types.
- `RoomService` is not a Spring `@Component` — it is instantiated manually in `AppConfig` to keep the domain
  Spring-free.
- The `RoomController` returns `Room` domain objects directly for GET endpoints, and `CreateRoomResponse` for POST.

## Tests

Three test layers, each in its own module:

| Layer       | Class                           | Module        | Annotation                                  |
|-------------|---------------------------------|---------------|---------------------------------------------|
| Domain unit | `RoomServiceTest`               | `domain`      | none (plain JUnit 5 + mockito-kotlin)       |
| Web slice   | `RoomControllerWebMvcTest`      | `adapter-web` | `@WebMvcTest`                               |
| Integration | `RoomControllerIntegrationTest` | `bootstrap`   | `@SpringBootTest` + `@AutoConfigureMockMvc` |

- `@WebMvcTest` requires `TestWebApplication` (in `adapter-web/src/test`) as the `@SpringBootApplication` anchor for the
  web slice.
- Integration tests use `@BeforeEach roomJpaRepository.deleteAll()` for isolation.
- `mockito-kotlin` (v5.4.0) is used for mocking in domain and web tests.
- Test names (backtick strings in Kotlin) must be written in English.
