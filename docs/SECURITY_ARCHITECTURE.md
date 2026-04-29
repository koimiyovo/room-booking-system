# Security Architecture

## Overview

The room booking system uses **JWT (JSON Web Tokens)** with **Spring Security** for stateless authentication and role-based authorization. The implementation follows the hexagonal architecture: security concerns are an adapter-web detail; the domain remains Spring-free.

---

## Authentication Flows

### Registration

```
Client                     AuthController           UserService         BCryptPasswordHashAdapter
  │                              │                       │                        │
  │  POST /api/auth/register     │                       │                        │
  │ ─────────────────────────►  │                       │                        │
  │  { name, email, password }   │                       │                        │
  │                              │  save(NewUser)        │                        │
  │                              │ ─────────────────►   │                        │
  │                              │                       │  hash(rawPassword)     │
  │                              │                       │ ──────────────────►   │
  │                              │                       │  UserPassword(hash)    │
  │                              │                       │ ◄──────────────────   │
  │                              │                       │  userRepository.save() │
  │  201 { id, name, email,      │                       │                        │
  │        role (no password) }  │                       │                        │
  │ ◄─────────────────────────  │                       │                        │
```

### Login

```
Client                     AuthController           UserService      BCryptPasswordHashAdapter   JwtService
  │                              │                       │                    │                      │
  │  POST /api/auth/login        │                       │                    │                      │
  │ ─────────────────────────►  │                       │                    │                      │
  │  { email, password }         │                       │                    │                      │
  │                              │  findByEmail(email)   │                    │                      │
  │                              │ ─────────────────►   │                    │                      │
  │                              │  User (with hash)     │                    │                      │
  │                              │ ◄─────────────────   │                    │                      │
  │                              │  matches(raw, hash)   │                    │                      │
  │                              │ ──────────────────────────────────────►  │                      │
  │                              │  true / false          │                    │                      │
  │                              │ ◄──────────────────────────────────────  │                      │
  │                              │  generateToken(userId, role)               │                      │
  │                              │ ──────────────────────────────────────────────────────────────► │
  │                              │  JWT token             │                    │                      │
  │                              │ ◄─────────────────────────────────────────────────────────────  │
  │  200 { token }               │                       │                    │                      │
  │ ◄─────────────────────────  │                       │                    │                      │
```

### Authenticated Request

```
Client              JwtAuthenticationFilter      SecurityConfig         Controller
  │                         │                          │                     │
  │  GET /api/bookings/my   │                          │                     │
  │  Authorization: Bearer  │                          │                     │
  │  <token>                │                          │                     │
  │ ───────────────────►   │                          │                     │
  │                         │  validateToken(token)    │                     │
  │                         │  extractUserId → UUID    │                     │
  │                         │  extractRole → USER      │                     │
  │                         │  set SecurityContext     │                     │
  │                         │  (userId, ROLE_USER)     │                     │
  │                         │ ────────────────────►   │                     │
  │                         │                          │  check rules        │
  │                         │                          │  /my → authenticated│
  │                         │                          │  ✓ authorized       │
  │                         │                          │ ──────────────────► │
  │                         │                          │                     │  authentication.name
  │                         │                          │                     │  → userId
  │                         │                          │                     │  findByUserId(userId)
  │  200 [ bookings ]       │                          │                     │
  │ ◄──────────────────────────────────────────────────────────────────────  │
```

---

## Components

### adapter-web

| Component | Role |
|---|---|
| `JwtService` | Generates JWT tokens (sign) and validates/parses them. Reads secret and expiry from `application.yml`. |
| `JwtAuthenticationFilter` | `OncePerRequestFilter` — extracts the `Bearer` token from the `Authorization` header, validates it, sets the `SecurityContext` with `userId` as principal and `ROLE_XXX` as authority. |
| `SecurityConfig` | Defines the `SecurityFilterChain`: disables CSRF, stateless sessions, registers rules per endpoint and role, adds `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`. |
| `BCryptPasswordHashAdapter` | Implements `PasswordHashPort` (domain port) using `BCryptPasswordEncoder`. The domain knows only the port; BCrypt is an adapter-web detail. |
| `AuthController` | `POST /api/auth/register` and `POST /api/auth/login` — public endpoints, no token required. |

### domain

| Component | Role |
|---|---|
| `PasswordHashPort` | Secondary port — abstracts password hashing. Implemented by `BCryptPasswordHashAdapter`. |
| `UserRole` | Enum `USER` / `ADMIN` stored on the `User` aggregate and embedded in the JWT. |
| `UserPassword` | Value object wrapping the BCrypt hash. Never returned to clients. |

---

## JWT Token Structure

```
Header: { alg: HS256, typ: JWT }
Payload: {
  sub: "<userId UUID>",
  role: "USER" | "ADMIN",
  iat: <issued-at timestamp>,
  exp: <expiration timestamp>
}
Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
```

The `userId` is stored in `sub`. The `SecurityContext` principal name is set to this UUID string.

**Configuration** (`application.yml`):
```yaml
app:
  jwt:
    secret: <base64-encoded key of at least 256 bits>
    expiration-ms: 86400000  # 24 hours
```

---

## Authorization Matrix

| Endpoint | PUBLIC | USER | ADMIN |
|---|:---:|:---:|:---:|
| `POST /api/auth/register` | ✓ | ✓ | ✓ |
| `POST /api/auth/login` | ✓ | ✓ | ✓ |
| `GET /api/users` | | | ✓ |
| `GET /api/users/{id}` | | | ✓ |
| `PUT /api/users/{id}` | | own account | ✓ |
| `DELETE /api/users/{id}` | | own account | ✓ |
| `GET /api/rooms` | | ✓ | ✓ |
| `GET /api/rooms/{id}` | | ✓ | ✓ |
| `POST /api/rooms` | | | ✓ |
| `GET /api/bookings` | | | ✓ |
| `GET /api/bookings/my` | | ✓ | ✓ |
| `GET /api/bookings/{id}` | | own booking | ✓ |
| `POST /api/bookings` | | ✓ | ✓ |
| `POST /api/bookings/{id}/cancel` | | own booking | ✓ (any) |

**Ownership checks** are enforced at the controller level (reading `authentication.name` which contains the userId). For `cancel`, passing `null` as `requestingUserId` in `BookingService` bypasses the ownership check (admin path).

---

## Hexagonal Architecture of Security Components

```
┌─────────────────────────────────────────────────────────────┐
│                        adapter-web                          │
│                                                             │
│  JwtAuthenticationFilter ──► JwtService                    │
│         │                                                   │
│         ▼                                                   │
│  SecurityConfig (SecurityFilterChain)                       │
│         │                                                   │
│         ▼                                                   │
│  Controller  ──────────────────────────────────────────┐   │
│                                                         │   │
│  BCryptPasswordHashAdapter ◄── PasswordHashPort (port)  │   │
│                                     (domain)            │   │
└─────────────────────────────────────────────────────────┼───┘
                                                          │
┌─────────────────────────────────────────────────────────┼───┐
│                         domain                          │   │
│                                                         │   │
│  UserService ◄── PasswordHashPort                       │   │
│       │                                                 ▼   │
│       └──► UserRepository           BookingService ◄───┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

The domain (`UserService`, `BookingService`) knows only the `PasswordHashPort` interface. Spring Security, BCrypt, and JWT are invisible to the domain.
