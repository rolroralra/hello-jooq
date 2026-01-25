# Chapter 5: JOIN과 서브쿼리

## 개요

이 챕터에서는 jOOQ를 사용해 테이블 간의 관계를 다루는 JOIN과 서브쿼리를 작성하는 방법을 배웁니다.

## 테이블 예시

이 챕터의 예제에서는 다음 테이블들을 사용합니다:

```sql
-- 사용자 테이블
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(255)
);

-- 주문 테이블
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    total_amount DECIMAL(10, 2),
    status VARCHAR(20),
    created_at TIMESTAMP
);

-- 주문 상품 테이블
CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id),
    product_id INTEGER REFERENCES products(id),
    quantity INTEGER,
    price DECIMAL(10, 2)
);

-- 상품 테이블
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    category_id INTEGER REFERENCES categories(id),
    price DECIMAL(10, 2)
);
```

## INNER JOIN

두 테이블에서 조건이 일치하는 행만 반환합니다.

```kotlin
// SQL: SELECT users.*, orders.*
//      FROM users
//      INNER JOIN orders ON users.id = orders.user_id
val result = dsl.select()
    .from(USERS)
    .join(ORDERS).on(USERS.ID.eq(ORDERS.USER_ID))
    .fetch()

// 특정 컬럼만 선택
val result = dsl.select(USERS.NAME, ORDERS.TOTAL_AMOUNT, ORDERS.STATUS)
    .from(USERS)
    .join(ORDERS).on(USERS.ID.eq(ORDERS.USER_ID))
    .fetch()

result.forEach { record ->
    val userName = record[USERS.NAME]
    val amount = record[ORDERS.TOTAL_AMOUNT]
    println("$userName: $amount")
}
```

## LEFT JOIN

왼쪽 테이블의 모든 행과, 일치하는 오른쪽 테이블의 행을 반환합니다.

```kotlin
// SQL: SELECT users.name, orders.id, orders.total_amount
//      FROM users
//      LEFT JOIN orders ON users.id = orders.user_id
val result = dsl.select(USERS.NAME, ORDERS.ID, ORDERS.TOTAL_AMOUNT)
    .from(USERS)
    .leftJoin(ORDERS).on(USERS.ID.eq(ORDERS.USER_ID))
    .fetch()

// 주문이 없는 사용자 찾기
val usersWithoutOrders = dsl.select(USERS.NAME)
    .from(USERS)
    .leftJoin(ORDERS).on(USERS.ID.eq(ORDERS.USER_ID))
    .where(ORDERS.ID.isNull)
    .fetch()
```

## RIGHT JOIN

오른쪽 테이블의 모든 행과, 일치하는 왼쪽 테이블의 행을 반환합니다.

```kotlin
// SQL: SELECT users.name, orders.*
//      FROM users
//      RIGHT JOIN orders ON users.id = orders.user_id
val result = dsl.select(USERS.NAME, ORDERS.asterisk())
    .from(USERS)
    .rightJoin(ORDERS).on(USERS.ID.eq(ORDERS.USER_ID))
    .fetch()
```

## FULL OUTER JOIN

양쪽 테이블의 모든 행을 반환합니다.

```kotlin
// SQL: SELECT * FROM users FULL OUTER JOIN orders ON users.id = orders.user_id
val result = dsl.select()
    .from(USERS)
    .fullOuterJoin(ORDERS).on(USERS.ID.eq(ORDERS.USER_ID))
    .fetch()
```

## 다중 JOIN

```kotlin
// 사용자 -> 주문 -> 주문 상품 -> 상품
val result = dsl
    .select(
        USERS.NAME,
        ORDERS.ID,
        PRODUCTS.NAME,
        ORDER_ITEMS.QUANTITY,
        ORDER_ITEMS.PRICE
    )
    .from(USERS)
    .join(ORDERS).on(USERS.ID.eq(ORDERS.USER_ID))
    .join(ORDER_ITEMS).on(ORDERS.ID.eq(ORDER_ITEMS.ORDER_ID))
    .join(PRODUCTS).on(ORDER_ITEMS.PRODUCT_ID.eq(PRODUCTS.ID))
    .where(ORDERS.STATUS.eq("COMPLETED"))
    .fetch()
```

## SELF JOIN

```kotlin
// 직원 테이블에서 매니저 이름 조회
// SQL: SELECT e.name as employee, m.name as manager
//      FROM employees e
//      LEFT JOIN employees m ON e.manager_id = m.id
val e = EMPLOYEES.`as`("e")
val m = EMPLOYEES.`as`("m")

val result = dsl.select(
        e.NAME.`as`("employee"),
        m.NAME.`as`("manager")
    )
    .from(e)
    .leftJoin(m).on(e.MANAGER_ID.eq(m.ID))
    .fetch()
```

## CROSS JOIN

```kotlin
// SQL: SELECT * FROM products CROSS JOIN categories
val result = dsl.select()
    .from(PRODUCTS)
    .crossJoin(CATEGORIES)
    .fetch()
```

## 서브쿼리 - SELECT 절

```kotlin
// SQL: SELECT name,
//             (SELECT COUNT(*) FROM orders WHERE orders.user_id = users.id) as order_count
//      FROM users
val orderCount = dsl.selectCount()
    .from(ORDERS)
    .where(ORDERS.USER_ID.eq(USERS.ID))

val result = dsl.select(
        USERS.NAME,
        orderCount.asField("order_count")
    )
    .from(USERS)
    .fetch()
```

## 서브쿼리 - WHERE 절

### IN 서브쿼리

```kotlin
// SQL: SELECT * FROM users
//      WHERE id IN (SELECT user_id FROM orders WHERE status = 'COMPLETED')
val completedOrderUserIds = dsl.select(ORDERS.USER_ID)
    .from(ORDERS)
    .where(ORDERS.STATUS.eq("COMPLETED"))

val result = dsl.selectFrom(USERS)
    .where(USERS.ID.`in`(completedOrderUserIds))
    .fetch()
```

### EXISTS 서브쿼리

```kotlin
// SQL: SELECT * FROM users u
//      WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id)
val u = USERS.`as`("u")
val o = ORDERS.`as`("o")

val existsSubquery = dsl.selectOne()
    .from(o)
    .where(o.USER_ID.eq(u.ID))

val result = dsl.selectFrom(u)
    .whereExists(existsSubquery)
    .fetch()

// NOT EXISTS
val usersWithoutOrders = dsl.selectFrom(u)
    .whereNotExists(existsSubquery)
    .fetch()
```

### 비교 서브쿼리

```kotlin
// SQL: SELECT * FROM products
//      WHERE price > (SELECT AVG(price) FROM products)
val avgPrice = dsl.select(avg(PRODUCTS.PRICE))
    .from(PRODUCTS)

val expensiveProducts = dsl.selectFrom(PRODUCTS)
    .where(PRODUCTS.PRICE.gt(avgPrice))
    .fetch()
```

## 서브쿼리 - FROM 절 (인라인 뷰)

```kotlin
// SQL: SELECT * FROM (
//          SELECT user_id, SUM(total_amount) as total
//          FROM orders
//          GROUP BY user_id
//      ) AS order_totals
//      WHERE total > 1000
val orderTotals = dsl
    .select(ORDERS.USER_ID, sum(ORDERS.TOTAL_AMOUNT).`as`("total"))
    .from(ORDERS)
    .groupBy(ORDERS.USER_ID)
    .asTable("order_totals")

val result = dsl.select()
    .from(orderTotals)
    .where(orderTotals.field("total", BigDecimal::class.java)?.gt(BigDecimal(1000)))
    .fetch()
```

## CTE (Common Table Expression)

```kotlin
// SQL: WITH high_value_orders AS (
//          SELECT * FROM orders WHERE total_amount > 1000
//      )
//      SELECT users.*, high_value_orders.*
//      FROM users
//      JOIN high_value_orders ON users.id = high_value_orders.user_id

val highValueOrders = name("high_value_orders").`as`(
    dsl.selectFrom(ORDERS)
        .where(ORDERS.TOTAL_AMOUNT.gt(BigDecimal(1000)))
)

val result = dsl.with(highValueOrders)
    .select()
    .from(USERS)
    .join(highValueOrders).on(
        USERS.ID.eq(highValueOrders.field(ORDERS.USER_ID))
    )
    .fetch()
```

## 재귀 CTE

```kotlin
// 계층 구조 (예: 조직도) 조회
// SQL: WITH RECURSIVE org_tree AS (
//          SELECT id, name, manager_id, 1 as level
//          FROM employees WHERE manager_id IS NULL
//          UNION ALL
//          SELECT e.id, e.name, e.manager_id, t.level + 1
//          FROM employees e
//          JOIN org_tree t ON e.manager_id = t.id
//      )
//      SELECT * FROM org_tree

val orgTree = name("org_tree").fields("id", "name", "manager_id", "level").`as`(
    dsl.select(EMPLOYEES.ID, EMPLOYEES.NAME, EMPLOYEES.MANAGER_ID, inline(1))
        .from(EMPLOYEES)
        .where(EMPLOYEES.MANAGER_ID.isNull)
    .unionAll(
        dsl.select(
                EMPLOYEES.ID,
                EMPLOYEES.NAME,
                EMPLOYEES.MANAGER_ID,
                field(name("org_tree", "level"), Int::class.java).plus(1)
            )
            .from(EMPLOYEES)
            .join(table(name("org_tree")))
            .on(EMPLOYEES.MANAGER_ID.eq(field(name("org_tree", "id"), Int::class.java)))
    )
)

val result = dsl.withRecursive(orgTree)
    .selectFrom(orgTree)
    .fetch()
```

## UNION / INTERSECT / EXCEPT

```kotlin
// UNION - 중복 제거
val activeUsers = dsl.select(USERS.ID, USERS.NAME).from(USERS).where(USERS.STATUS.eq("ACTIVE"))
val premiumUsers = dsl.select(USERS.ID, USERS.NAME).from(USERS).where(USERS.TIER.eq("PREMIUM"))

val result = activeUsers.union(premiumUsers).fetch()

// UNION ALL - 중복 포함
val resultWithDuplicates = activeUsers.unionAll(premiumUsers).fetch()

// INTERSECT - 교집합
val activePremiumUsers = activeUsers.intersect(premiumUsers).fetch()

// EXCEPT - 차집합
val activeNonPremiumUsers = activeUsers.except(premiumUsers).fetch()
```

## 실전 예제: 복잡한 보고서 쿼리

```kotlin
// 월별 사용자별 주문 현황
fun getMonthlyOrderReport(year: Int, month: Int): List<MonthlyOrderReport> {
    val startDate = LocalDate.of(year, month, 1).atStartOfDay()
    val endDate = startDate.plusMonths(1)

    return dsl
        .select(
            USERS.ID,
            USERS.NAME,
            count(ORDERS.ID).`as`("order_count"),
            sum(ORDERS.TOTAL_AMOUNT).`as`("total_amount"),
            avg(ORDERS.TOTAL_AMOUNT).`as`("avg_amount")
        )
        .from(USERS)
        .leftJoin(ORDERS).on(
            USERS.ID.eq(ORDERS.USER_ID)
                .and(ORDERS.CREATED_AT.between(startDate, endDate))
        )
        .groupBy(USERS.ID, USERS.NAME)
        .orderBy(sum(ORDERS.TOTAL_AMOUNT).desc().nullsLast())
        .fetchInto(MonthlyOrderReport::class.java)
}

data class MonthlyOrderReport(
    val id: Int,
    val name: String,
    val orderCount: Int,
    val totalAmount: BigDecimal?,
    val avgAmount: BigDecimal?
)
```

## 다음 단계

다음 챕터에서는 Spring Boot와 jOOQ를 통합하는 방법을 더 자세히 알아보겠습니다.

[← Chapter 4: INSERT, UPDATE, DELETE](./04-data-manipulation.md) | [Chapter 6: Spring Boot 통합 →](./06-spring-boot-integration.md)
