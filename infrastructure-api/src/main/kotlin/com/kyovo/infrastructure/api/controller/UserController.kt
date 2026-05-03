package com.kyovo.infrastructure.api.controller

import com.kyovo.domain.model.user.UserId
import com.kyovo.domain.model.user.UserRole.ADMIN
import com.kyovo.domain.model.user.UserStatusReason
import com.kyovo.domain.port.primary.UserUseCase
import com.kyovo.infrastructure.api.dto.UpdateUserRequest
import com.kyovo.infrastructure.api.dto.UserResponse
import com.kyovo.infrastructure.api.dto.UserStatusReasonRequest
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
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "User list returned successfully"),
        ApiResponse(responseCode = "401", description = "Authentication required"),
        ApiResponse(responseCode = "403", description = "Admin role required")
    )
    fun findAll(): List<UserResponse>
    {
        return userUseCase.findAll().map { UserResponse.fromDomain(it) }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by its identifier")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "User found"),
        ApiResponse(responseCode = "401", description = "Authentication required"),
        ApiResponse(responseCode = "403", description = "Admin role required"),
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
        ApiResponse(responseCode = "401", description = "Authentication required"),
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
        ApiResponse(responseCode = "401", description = "Authentication required"),
        ApiResponse(responseCode = "403", description = "Access denied"),
        ApiResponse(responseCode = "404", description = "User not found")
    )
    fun delete(
        @PathVariable id: UUID,
        @RequestBody(required = false) request: UserStatusReasonRequest?,
        authentication: Authentication
    ): ResponseEntity<Void>
    {
        val targetId = UserId(id)
        if (!isAdminOrOwner(authentication, targetId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        userUseCase.delete(targetId, request?.reason?.let { UserStatusReason(it) })
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Validate a user account")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Account validated successfully"),
        ApiResponse(responseCode = "401", description = "Authentication required"),
        ApiResponse(responseCode = "403", description = "Access denied"),
        ApiResponse(responseCode = "404", description = "User not found"),
        ApiResponse(responseCode = "409", description = "Invalid status transition")
    )
    fun validate(
        @Parameter(description = "UUID identifier of the user")
        @PathVariable id: UUID,
        @RequestBody(required = false) request: UserStatusReasonRequest?,
        authentication: Authentication
    ): ResponseEntity<UserResponse>
    {
        val targetId = UserId(id)
        val isAdmin = authentication.authorities.any { it.authority == "ROLE_${ADMIN.label}" }
        val validateBy = UserId(UUID.fromString(authentication.name))
        val user = userUseCase.validate(targetId, isAdmin, validateBy, request?.reason?.let { UserStatusReason(it) })
        return ResponseEntity.ok(UserResponse.fromDomain(user))
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a user account")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Account deactivated successfully"),
        ApiResponse(responseCode = "401", description = "Authentication required"),
        ApiResponse(responseCode = "403", description = "Admin role required"),
        ApiResponse(responseCode = "404", description = "User not found"),
        ApiResponse(responseCode = "409", description = "Invalid status transition")
    )
    fun deactivate(
        @Parameter(description = "UUID identifier of the user")
        @PathVariable id: UUID,
        @RequestBody(required = false) request: UserStatusReasonRequest?,
        authentication: Authentication
    ): ResponseEntity<UserResponse>
    {
        if (!isAdmin(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val user = userUseCase.deactivate(UserId(id), request?.reason?.let { UserStatusReason(it) })
        return ResponseEntity.ok(UserResponse.fromDomain(user))
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate a user account")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Account reactivated successfully"),
        ApiResponse(responseCode = "401", description = "Authentication required"),
        ApiResponse(responseCode = "403", description = "Admin role required"),
        ApiResponse(responseCode = "404", description = "User not found"),
        ApiResponse(responseCode = "409", description = "Invalid status transition")
    )
    fun reactivate(
        @Parameter(description = "UUID identifier of the user")
        @PathVariable id: UUID,
        @RequestBody(required = false) request: UserStatusReasonRequest?,
        authentication: Authentication
    ): ResponseEntity<UserResponse>
    {
        if (!isAdmin(authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        val user = userUseCase.reactivate(UserId(id), request?.reason?.let { UserStatusReason(it) })
        return ResponseEntity.ok(UserResponse.fromDomain(user))
    }

    private fun isAdmin(authentication: Authentication): Boolean =
        authentication.authorities.any { it.authority == "ROLE_${ADMIN.label}" }

    private fun isAdminOrOwner(authentication: Authentication, targetId: UserId): Boolean =
        isAdmin(authentication) || UserId(UUID.fromString(authentication.name)) == targetId
}
