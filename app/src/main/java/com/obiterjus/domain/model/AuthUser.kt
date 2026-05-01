package com.obiterjus.domain.model

data class AuthUser(
    val uid: String,
    val email: String?,
    val isAnonymous: Boolean,
)
