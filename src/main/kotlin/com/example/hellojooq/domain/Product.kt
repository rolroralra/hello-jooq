package com.example.hellojooq.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class Product(
    val id: Long? = null,
    val name: String,
    val description: String? = null,
    val price: BigDecimal,
    val stockQuantity: Int = 0,
    val categoryId: Long? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

data class Category(
    val id: Long? = null,
    val name: String,
    val description: String? = null
)
