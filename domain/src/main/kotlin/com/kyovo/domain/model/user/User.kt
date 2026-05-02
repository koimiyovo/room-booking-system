package com.kyovo.domain.model.user

data class User(
    val id: UserId,
    val name: UserName,
    val email: UserEmail,
    val password: UserPassword,
    val role: UserRole,
    val registeredAt: UserRegistrationDate,
    val statusInfo: UserStatusInfo
)
{
    fun isInactive(): Boolean
    {
        return statusInfo.isInactive()
    }
}
