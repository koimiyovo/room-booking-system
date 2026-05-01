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
        return User(UserId.generate(), name, email, password, role, registrationDate)
    }
}
