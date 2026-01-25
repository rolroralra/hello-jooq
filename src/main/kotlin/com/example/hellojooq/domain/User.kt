package com.example.hellojooq.domain

import java.time.LocalDateTime

data class User(
    val id: Long? = null,
    val name: String,
    val email: String,
    val status: UserStatus = UserStatus.ACTIVE,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

enum class UserStatus {
    ACTIVE, INACTIVE, DELETED
}
