package com.kyovo.domain.model

data class UpdateUser(
    val name: UserName?,
    val email: UserEmail?,
    val password: UserPassword?
)
