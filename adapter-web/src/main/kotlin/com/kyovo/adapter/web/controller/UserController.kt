package com.kyovo.adapter.web.controller

import com.kyovo.adapter.web.dto.CreateUserRequest
import com.kyovo.adapter.web.dto.CreateUserResponse
import com.kyovo.adapter.web.dto.UserResponse
import com.kyovo.domain.model.UserId
import com.kyovo.domain.port.primary.UserUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management")
class UserController(private val userUseCase: UserUseCase)
{
    @GetMapping
    @Operation(summary = "List all users")
    @ApiResponse(responseCode = "200", description = "User list returned successfully")
    fun findAll(): List<UserResponse>
    {
        return userUseCase.findAll().map { UserResponse.fromDomain(it) }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by its identifier")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "User found"),
        ApiResponse(responseCode = "404", description = "User not found")
    )
    fun findById(
        @Parameter(description = "UUID identifier of the user")
        @PathVariable id: UUID
    ): ResponseEntity<UserResponse>
    {
        return userUseCase.findById(UserId(id))
            ?.let { ResponseEntity.ok(UserResponse.fromDomain(it)) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    @ApiResponse(responseCode = "201", description = "User created successfully")
    fun create(@RequestBody request: CreateUserRequest): ResponseEntity<CreateUserResponse>
    {
        val user = userUseCase.save(request.toNewUser())
        val response = CreateUserResponse(
            id = user.id.value,
            name = user.name.value,
            email = user.email.value
        )
        return ResponseEntity(response, HttpStatus.CREATED)
    }
}
