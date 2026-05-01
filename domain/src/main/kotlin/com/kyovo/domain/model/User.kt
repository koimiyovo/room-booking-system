package com.kyovo.domain.model

data class User(
    val id: UserId,
    val name: UserName,
    val email: UserEmail,
    val password: UserPassword,
    val role: UserRole,
    val registeredAt: UserRegistrationDate
)
