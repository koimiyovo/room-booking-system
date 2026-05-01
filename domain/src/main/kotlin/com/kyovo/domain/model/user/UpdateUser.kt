package com.kyovo.domain.model.user

data class UpdateUser(
    val name: UserName?,
    val email: UserEmail?,
    val password: UserPassword?
)
