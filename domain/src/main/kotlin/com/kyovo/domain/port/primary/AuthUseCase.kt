package com.kyovo.domain.port.primary

import com.kyovo.domain.model.NewUser
import com.kyovo.domain.model.User
import com.kyovo.domain.model.UserEmail

interface AuthUseCase
{
    fun register(newUser: NewUser): User

    fun login(email: UserEmail, rawPassword: String): User
}
