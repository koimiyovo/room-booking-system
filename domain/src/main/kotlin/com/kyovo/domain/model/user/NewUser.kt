package com.kyovo.domain.model.user

data class NewUser(
    val name: UserName,
    val email: UserEmail,
    val password: UserPassword,
    val role: UserRole = UserRole.USER
)
{
    fun toUser(registrationDate: UserRegistrationDate): User
    {
        return User(
            id = UserId.generate(),
            name = name,
            email = email,
            password = password,
            role = role,
            registeredAt = registrationDate,
            statusInfo = UserStatusInfo(status = UserStatus.CREATED, since = UserStatusInfoDate(registrationDate.value))
        )
    }
}
