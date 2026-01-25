package com.example.hellojooq.service

import com.example.hellojooq.domain.Order
import com.example.hellojooq.domain.OrderItem
import com.example.hellojooq.domain.OrderStatus
import com.example.hellojooq.repository.OrderItemDetail
import com.example.hellojooq.repository.OrderRepository
import com.example.hellojooq.repository.OrderWithUser
import com.example.hellojooq.repository.UserOrderSummary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val productService: ProductService
) {
    fun findById(id: Long): Order {
        return orderRepository.findById(id)
            ?: throw NoSuchElementException("Order not found: $id")
    }

    fun findByUserId(userId: Long): List<Order> {
        return orderRepository.findByUserId(userId)
    }

    fun findByStatus(status: OrderStatus): List<Order> {
        return orderRepository.findByStatus(status)
    }

    @Transactional
    fun createOrder(request: CreateOrderRequest): Order {
        // 주문 상품의 가격 조회 및 총액 계산
        val items = request.items.map { item ->
            val product = productService.findById(item.productId)
            OrderItem(
                productId = item.productId,
                quantity = item.quantity,
                price = product.price
            )
        }

        val totalAmount = items.sumOf { it.price.multiply(BigDecimal(it.quantity)) }

        val order = Order(
            userId = request.userId,
            totalAmount = totalAmount,
            status = OrderStatus.PENDING,
            items = items
        )

        return orderRepository.save(order)
    }

    @Transactional
    fun updateOrderStatus(orderId: Long, status: OrderStatus) {
        if (!orderRepository.updateStatus(orderId, status)) {
            throw NoSuchElementException("Order not found: $orderId")
        }
    }

    @Transactional
    fun confirmOrder(orderId: Long) {
        updateOrderStatus(orderId, OrderStatus.CONFIRMED)
    }

    @Transactional
    fun completeOrder(orderId: Long) {
        updateOrderStatus(orderId, OrderStatus.COMPLETED)
    }

    @Transactional
    fun cancelOrder(orderId: Long) {
        updateOrderStatus(orderId, OrderStatus.CANCELLED)
    }

    // 조회 기능
    fun getOrdersWithUserInfo(): List<OrderWithUser> {
        return orderRepository.findOrdersWithUserInfo()
    }

    fun getOrderItemDetails(orderId: Long): List<OrderItemDetail> {
        return orderRepository.findOrderItemsWithProductInfo(orderId)
    }

    fun getUserOrderSummary(): List<UserOrderSummary> {
        return orderRepository.getTotalAmountByUser()
    }
}

data class CreateOrderRequest(
    val userId: Long,
    val items: List<OrderItemRequest>
)

data class OrderItemRequest(
    val productId: Long,
    val quantity: Int
)
