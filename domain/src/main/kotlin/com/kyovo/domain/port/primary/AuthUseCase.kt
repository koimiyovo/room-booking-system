package com.kyovo.domain.port.primary

import com.kyovo.domain.model.user.NewUser
import com.kyovo.domain.model.user.User
import com.kyovo.domain.model.user.UserEmail

interface AuthUseCase
{
    fun register(newUser: NewUser): User

    fun login(email: UserEmail, rawPassword: String): User
}
