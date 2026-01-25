package com.example.hellojooq.domain

import java.math.BigDecimal
import java.time.LocalDateTime

data class Order(
    val id: Long? = null,
    val userId: Long,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val items: List<OrderItem> = emptyList()
)

data class OrderItem(
    val id: Long? = null,
    val orderId: Long? = null,
    val productId: Long,
    val quantity: Int,
    val price: BigDecimal
)

enum class OrderStatus {
    PENDING, CONFIRMED, SHIPPED, COMPLETED, CANCELLED
}
