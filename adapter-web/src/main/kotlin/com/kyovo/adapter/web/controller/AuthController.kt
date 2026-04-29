package com.kyovo.adapter.web.controller

import com.kyovo.adapter.web.dto.*
import com.kyovo.adapter.web.security.JwtService
import com.kyovo.domain.model.UserEmail
import com.kyovo.domain.exception.InvalidCredentialsException
import com.kyovo.domain.port.primary.UserUseCase
import com.kyovo.domain.port.secondary.PasswordHashPort
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Account registration and login")
class AuthController(
    private val userUseCase: UserUseCase,
    private val passwordHashPort: PasswordHashPort,
    private val jwtService: JwtService
)
{
    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Account created successfully"),
        ApiResponse(responseCode = "409", description = "Email already in use")
    )
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<UserResponse>
    {
        val user = userUseCase.save(request.toNewUser())
        return ResponseEntity(UserResponse.fromDomain(user), HttpStatus.CREATED)
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT token")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Login successful"),
        ApiResponse(responseCode = "401", description = "Invalid credentials")
    )
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse>
    {
        val user = userUseCase.findByEmail(UserEmail(request.email))
            ?: throw InvalidCredentialsException()
        if (!passwordHashPort.matches(request.password, user.password)) throw InvalidCredentialsException()
        val token = jwtService.generateToken(user.id, user.role)
        return ResponseEntity.ok(LoginResponse(token))
    }
}
