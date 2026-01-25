package com.example.hellojooq.repository

import com.example.hellojooq.domain.Order
import com.example.hellojooq.domain.OrderItem
import com.example.hellojooq.domain.OrderStatus
import com.example.hellojooq.generated.tables.OrderItems.Companion.ORDER_ITEMS
import com.example.hellojooq.generated.tables.Orders.Companion.ORDERS
import com.example.hellojooq.generated.tables.Products.Companion.PRODUCTS
import com.example.hellojooq.generated.tables.Users.Companion.USERS
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class OrderRepository(
    private val dsl: DSLContext
) {
    fun findById(id: Long): Order? {
        val order = dsl.selectFrom(ORDERS)
            .where(ORDERS.ID.eq(id))
            .fetchOneInto(Order::class.java) ?: return null

        val items = findOrderItems(id)
        return order.copy(items = items)
    }

    fun findByUserId(userId: Long): List<Order> {
        return dsl.selectFrom(ORDERS)
            .where(ORDERS.USER_ID.eq(userId))
            .orderBy(ORDERS.CREATED_AT.desc())
            .fetchInto(Order::class.java)
    }

    fun findByStatus(status: OrderStatus): List<Order> {
        return dsl.selectFrom(ORDERS)
            .where(ORDERS.STATUS.eq(status.name))
            .orderBy(ORDERS.CREATED_AT.desc())
            .fetchInto(Order::class.java)
    }

    fun findOrderItems(orderId: Long): List<OrderItem> {
        return dsl.selectFrom(ORDER_ITEMS)
            .where(ORDER_ITEMS.ORDER_ID.eq(orderId))
            .fetchInto(OrderItem::class.java)
    }

    @Transactional
    fun save(order: Order): Order {
        val now = LocalDateTime.now()

        // 주문 생성
        val orderRecord = dsl.insertInto(ORDERS)
            .set(ORDERS.USER_ID, order.userId)
            .set(ORDERS.TOTAL_AMOUNT, order.totalAmount)
            .set(ORDERS.STATUS, order.status.name)
            .set(ORDERS.CREATED_AT, now)
            .set(ORDERS.UPDATED_AT, now)
            .returning()
            .fetchOne() ?: throw RuntimeException("Failed to create order")

        val orderId = orderRecord.id!!

        // 주문 상품 추가
        order.items.forEach { item ->
            dsl.insertInto(ORDER_ITEMS)
                .set(ORDER_ITEMS.ORDER_ID, orderId)
                .set(ORDER_ITEMS.PRODUCT_ID, item.productId)
                .set(ORDER_ITEMS.QUANTITY, item.quantity)
                .set(ORDER_ITEMS.PRICE, item.price)
                .execute()
        }

        return findById(orderId)!!
    }

    fun updateStatus(orderId: Long, status: OrderStatus): Boolean {
        return dsl.update(ORDERS)
            .set(ORDERS.STATUS, status.name)
            .set(ORDERS.UPDATED_AT, LocalDateTime.now())
            .where(ORDERS.ID.eq(orderId))
            .execute() > 0
    }

    // JOIN 예제: 주문과 사용자 정보 함께 조회
    fun findOrdersWithUserInfo(): List<OrderWithUser> {
        return dsl.select(
                ORDERS.ID,
                ORDERS.TOTAL_AMOUNT,
                ORDERS.STATUS,
                ORDERS.CREATED_AT,
                USERS.NAME.`as`("userName"),
                USERS.EMAIL.`as`("userEmail")
            )
            .from(ORDERS)
            .join(USERS).on(ORDERS.USER_ID.eq(USERS.ID))
            .orderBy(ORDERS.CREATED_AT.desc())
            .fetchInto(OrderWithUser::class.java)
    }

    // JOIN 예제: 주문 상품 상세 정보 조회
    fun findOrderItemsWithProductInfo(orderId: Long): List<OrderItemDetail> {
        return dsl.select(
                ORDER_ITEMS.ID,
                ORDER_ITEMS.QUANTITY,
                ORDER_ITEMS.PRICE,
                PRODUCTS.NAME.`as`("productName"),
                PRODUCTS.DESCRIPTION.`as`("productDescription")
            )
            .from(ORDER_ITEMS)
            .join(PRODUCTS).on(ORDER_ITEMS.PRODUCT_ID.eq(PRODUCTS.ID))
            .where(ORDER_ITEMS.ORDER_ID.eq(orderId))
            .fetchInto(OrderItemDetail::class.java)
    }

    // 집계 예제: 사용자별 총 주문 금액
    fun getTotalAmountByUser(): List<UserOrderSummary> {
        return dsl.select(
                USERS.ID,
                USERS.NAME,
                DSL.count(ORDERS.ID).`as`("orderCount"),
                DSL.sum(ORDERS.TOTAL_AMOUNT).`as`("totalAmount")
            )
            .from(USERS)
            .leftJoin(ORDERS).on(USERS.ID.eq(ORDERS.USER_ID))
            .groupBy(USERS.ID, USERS.NAME)
            .orderBy(DSL.sum(ORDERS.TOTAL_AMOUNT).desc().nullsLast())
            .fetchInto(UserOrderSummary::class.java)
    }
}

data class OrderWithUser(
    val id: Long,
    val totalAmount: BigDecimal,
    val status: String,
    val createdAt: LocalDateTime,
    val userName: String,
    val userEmail: String
)

data class OrderItemDetail(
    val id: Long,
    val quantity: Int,
    val price: BigDecimal,
    val productName: String,
    val productDescription: String?
)

data class UserOrderSummary(
    val id: Long,
    val name: String,
    val orderCount: Int,
    val totalAmount: BigDecimal?
)
