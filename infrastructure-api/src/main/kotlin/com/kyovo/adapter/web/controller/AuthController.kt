package com.kyovo.adapter.web.controller

import com.kyovo.adapter.web.dto.LoginRequest
import com.kyovo.adapter.web.dto.LoginResponse
import com.kyovo.adapter.web.dto.RegisterRequest
import com.kyovo.adapter.web.dto.RegisterResponse
import com.kyovo.adapter.web.security.AuthToken
import com.kyovo.adapter.web.security.JwtService
import com.kyovo.adapter.web.security.TokenBlacklistService
import com.kyovo.domain.exception.InvalidCredentialsException
import com.kyovo.domain.model.UserEmail
import com.kyovo.domain.port.primary.AuthUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Account registration and login")
class AuthController(
    private val authUseCase: AuthUseCase,
    private val jwtService: JwtService,
    private val tokenBlacklistService: TokenBlacklistService
)
{
    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Account created successfully"),
        ApiResponse(responseCode = "409", description = "Email already in use")
    )
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse>
    {
        val user = authUseCase.register(request.toNewUser())
        return ResponseEntity(RegisterResponse.fromDomain(user), HttpStatus.CREATED)
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT token")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Login successful"),
        ApiResponse(responseCode = "401", description = "Invalid credentials")
    )
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse>
    {
        val user = authUseCase.login(UserEmail(request.email), request.password)
        val token = jwtService.generateToken(user.id, user.role)
        return ResponseEntity.ok(LoginResponse(token))
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out and revoke the JWT token")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Logout successful"),
        ApiResponse(responseCode = "401", description = "Invalid or missing token")
    )
    fun logout(request: HttpServletRequest): ResponseEntity<Void>
    {
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer "))
        {
            val token = AuthToken(authHeader.substring(7))
            val expirationTime = jwtService.extractExpirationTime(token)
            if (expirationTime != null && jwtService.validateToken(token))
            {
                tokenBlacklistService.revokeToken(token, expirationTime)
                return ResponseEntity.ok().build()
            }
        }
        throw InvalidCredentialsException()
    }
}
