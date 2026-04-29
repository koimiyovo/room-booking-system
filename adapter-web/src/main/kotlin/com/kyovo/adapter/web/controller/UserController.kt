package com.kyovo.adapter.web.controller

import com.kyovo.adapter.web.dto.UpdateUserRequest
import com.kyovo.adapter.web.dto.UserResponse
import com.kyovo.domain.model.UserId
import com.kyovo.domain.model.UserRole.ADMIN
import com.kyovo.domain.port.primary.UserUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
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

    @PutMapping("/{id}")
    @Operation(summary = "Update a user account")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "User updated successfully"),
        ApiResponse(responseCode = "403", description = "Access denied"),
        ApiResponse(responseCode = "404", description = "User not found"),
        ApiResponse(responseCode = "409", description = "Email already in use")
    )
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateUserRequest,
        authentication: Authentication
    ): ResponseEntity<UserResponse>
    {
        val targetId = UserId(id)
        if (!isAdminOrOwner(authentication, targetId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val user = userUseCase.update(targetId, request.toUpdateUser())
        return ResponseEntity.ok(UserResponse.fromDomain(user))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user account")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "User deleted successfully"),
        ApiResponse(responseCode = "403", description = "Access denied"),
        ApiResponse(responseCode = "404", description = "User not found")
    )
    fun delete(
        @PathVariable id: UUID,
        authentication: Authentication
    ): ResponseEntity<Void>
    {
        val targetId = UserId(id)
        if (!isAdminOrOwner(authentication, targetId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        userUseCase.delete(targetId)
        return ResponseEntity.noContent().build()
    }

    private fun isAdminOrOwner(authentication: Authentication, targetId: UserId): Boolean
    {
        if (authentication.authorities.any { it.authority == "ROLE_${ADMIN.label}" }) return true
        return UserId(UUID.fromString(authentication.name)) == targetId
    }
}
