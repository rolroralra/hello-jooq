package com.example.hellojooq.controller

import com.example.hellojooq.domain.Order
import com.example.hellojooq.domain.OrderStatus
import com.example.hellojooq.repository.OrderItemDetail
import com.example.hellojooq.repository.OrderWithUser
import com.example.hellojooq.repository.UserOrderSummary
import com.example.hellojooq.service.CreateOrderRequest
import com.example.hellojooq.service.OrderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): Order {
        return orderService.findById(id)
    }

    @GetMapping("/user/{userId}")
    fun findByUserId(@PathVariable userId: Long): List<Order> {
        return orderService.findByUserId(userId)
    }

    @GetMapping("/status/{status}")
    fun findByStatus(@PathVariable status: OrderStatus): List<Order> {
        return orderService.findByStatus(status)
    }

    @PostMapping
    fun create(@RequestBody request: CreateOrderRequest): ResponseEntity<Order> {
        val order = orderService.createOrder(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(order)
    }

    @PatchMapping("/{id}/confirm")
    fun confirm(@PathVariable id: Long): ResponseEntity<Void> {
        orderService.confirmOrder(id)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/{id}/complete")
    fun complete(@PathVariable id: Long): ResponseEntity<Void> {
        orderService.completeOrder(id)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/{id}/cancel")
    fun cancel(@PathVariable id: Long): ResponseEntity<Void> {
        orderService.cancelOrder(id)
        return ResponseEntity.ok().build()
    }

    // JOIN 쿼리 결과 조회
    @GetMapping("/with-users")
    fun getOrdersWithUserInfo(): List<OrderWithUser> {
        return orderService.getOrdersWithUserInfo()
    }

    @GetMapping("/{id}/items/details")
    fun getOrderItemDetails(@PathVariable id: Long): List<OrderItemDetail> {
        return orderService.getOrderItemDetails(id)
    }

    // 집계 쿼리 결과 조회
    @GetMapping("/summary/by-user")
    fun getUserOrderSummary(): List<UserOrderSummary> {
        return orderService.getUserOrderSummary()
    }
}
