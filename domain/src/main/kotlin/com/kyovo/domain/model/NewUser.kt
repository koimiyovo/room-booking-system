package com.kyovo.domain.model

data class NewUser(
    val name: UserName,
    val email: UserEmail
)
{
    fun toUser(): User
    {
        return User(UserId.generate(), name, email)
    }
}
