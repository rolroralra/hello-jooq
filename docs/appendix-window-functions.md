# 부록: Window Functions (윈도우 함수)

> 이 문서는 참고용입니다. 집계 함수(GROUP BY)와 달리 행을 그룹으로 합치지 않고, 각 행에 대해 집계 계산을 할 수 있는 Window Functions를 다룹니다.

---

## Window Function이란?

```sql
-- 일반 집계: 그룹을 하나의 행으로 합침
SELECT department, AVG(salary) FROM employees GROUP BY department;

-- Window Function: 각 행을 유지하면서 집계값을 함께 표시
SELECT name, department, salary,
       AVG(salary) OVER (PARTITION BY department) AS dept_avg
FROM employees;
```

핵심 개념: `OVER()` 절이 붙으면 Window Function입니다.

```
RANK() OVER (
    PARTITION BY department    ← 어떤 기준으로 나눌지 (선택)
    ORDER BY salary DESC       ← 어떤 순서로 계산할지 (선택)
)
```

---

## jOOQ에서 Window Function 사용법

```kotlin
import org.jooq.impl.DSL.*   // rowNumber(), rank(), lag() 등 static import
```

### OVER() 기본 구조

```kotlin
// 집계함수.over()
sum(ORDERS.AMOUNT).over()

// PARTITION BY 추가
sum(ORDERS.AMOUNT).over().partitionBy(ORDERS.USER_ID)

// ORDER BY 추가
rowNumber().over().orderBy(ORDERS.CREATED_AT.asc())

// 둘 다
rank().over().partitionBy(ORDERS.USER_ID).orderBy(ORDERS.AMOUNT.desc())
```

---

## 순위 함수

### ROW_NUMBER() - 고유한 순번

```kotlin
// SQL: ROW_NUMBER() OVER (ORDER BY created_at ASC)
val result = dsl
    .select(
        ORDERS.ID,
        ORDERS.USER_ID,
        ORDERS.AMOUNT,
        rowNumber().over().orderBy(ORDERS.CREATED_AT.asc()).`as`("row_num")
    )
    .from(ORDERS)
    .fetch()

// 사용자별 순번 (PARTITION BY)
// SQL: ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at ASC)
val result = dsl
    .select(
        ORDERS.USER_ID,
        ORDERS.ID,
        ORDERS.AMOUNT,
        rowNumber()
            .over()
            .partitionBy(ORDERS.USER_ID)
            .orderBy(ORDERS.CREATED_AT.asc())
            .`as`("user_row_num")
    )
    .from(ORDERS)
    .fetch()
```

### RANK() vs DENSE_RANK() - 동점 처리 차이

```kotlin
// 점수가 같을 때:
// RANK():       1, 2, 2, 4  (3위 없음)
// DENSE_RANK(): 1, 2, 2, 3  (연속)

val result = dsl
    .select(
        USERS.NAME,
        USERS.SCORE,
        rank().over().orderBy(USERS.SCORE.desc()).`as`("rank"),
        denseRank().over().orderBy(USERS.SCORE.desc()).`as`("dense_rank")
    )
    .from(USERS)
    .fetch()
```

### PERCENT_RANK() / CUME_DIST() - 백분위

```kotlin
// 전체에서 상위 몇 퍼센트인지
val result = dsl
    .select(
        USERS.NAME,
        USERS.SCORE,
        percentRank().over().orderBy(USERS.SCORE.desc()).`as`("percent_rank"),
        cumeDist().over().orderBy(USERS.SCORE.desc()).`as`("cume_dist")
    )
    .from(USERS)
    .fetch()
```

---

## 집계 Window Functions

GROUP BY 없이 각 행에 집계값을 붙일 때 사용합니다.

```kotlin
// 사용자별 주문 통계를 각 주문 행에 함께 표시
val result = dsl
    .select(
        ORDERS.ID,
        ORDERS.USER_ID,
        ORDERS.AMOUNT,
        // 이 사용자의 총 주문 금액
        sum(ORDERS.AMOUNT)
            .over().partitionBy(ORDERS.USER_ID)
            .`as`("user_total"),
        // 이 사용자의 주문 수
        count().over().partitionBy(ORDERS.USER_ID).`as`("user_order_count"),
        // 전체 주문 대비 비율
        ORDERS.AMOUNT
            .div(sum(ORDERS.AMOUNT).over())
            .mul(100)
            .`as`("pct_of_total")
    )
    .from(ORDERS)
    .fetch()
```

---

## LAG() / LEAD() - 이전/다음 행 참조

시계열 비교, 변화량 계산에 유용합니다.

```kotlin
// 각 주문과 이전 주문 금액 비교
// SQL: LAG(amount, 1) OVER (PARTITION BY user_id ORDER BY created_at)
val result = dsl
    .select(
        ORDERS.USER_ID,
        ORDERS.CREATED_AT,
        ORDERS.AMOUNT,
        lag(ORDERS.AMOUNT, 1)
            .over()
            .partitionBy(ORDERS.USER_ID)
            .orderBy(ORDERS.CREATED_AT.asc())
            .`as`("prev_amount"),
        lead(ORDERS.AMOUNT, 1)
            .over()
            .partitionBy(ORDERS.USER_ID)
            .orderBy(ORDERS.CREATED_AT.asc())
            .`as`("next_amount")
    )
    .from(ORDERS)
    .fetch()

// 전월 대비 증감 계산
result.forEach { record ->
    val current = record.get("amount", BigDecimal::class.java)
    val prev = record.get("prev_amount", BigDecimal::class.java)
    val diff = if (prev != null) current.subtract(prev) else null
    println("변화량: $diff")
}
```

---

## FIRST_VALUE() / LAST_VALUE() / NTH_VALUE()

```kotlin
// 사용자의 첫 번째 주문 금액을 모든 행에 표시
val result = dsl
    .select(
        ORDERS.USER_ID,
        ORDERS.CREATED_AT,
        ORDERS.AMOUNT,
        firstValue(ORDERS.AMOUNT)
            .over()
            .partitionBy(ORDERS.USER_ID)
            .orderBy(ORDERS.CREATED_AT.asc())
            .`as`("first_order_amount"),
        lastValue(ORDERS.AMOUNT)
            .over()
            .partitionBy(ORDERS.USER_ID)
            .orderBy(ORDERS.CREATED_AT.asc())
            .rowsBetweenUnboundedPreceding()   // Frame 지정 (중요!)
            .andUnboundedFollowing()
            .`as`("last_order_amount")
    )
    .from(ORDERS)
    .fetch()
```

> **LAST_VALUE() 주의**: 기본 Frame이 `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW`이므로,
> 진짜 마지막 값을 원하면 반드시 Frame을 `UNBOUNDED FOLLOWING`으로 명시해야 합니다.

---

## Frame 절 (ROWS / RANGE)

Window의 범위를 세밀하게 제어할 때 사용합니다.

```kotlin
// 현재 행 포함 앞 2행의 이동 평균 (Moving Average)
sum(SALES.AMOUNT)
    .over()
    .orderBy(SALES.DATE.asc())
    .rowsBetween(
        WindowFrameUnits.ROWS.preceding(2),   // 2행 이전부터
        WindowFrameUnits.ROWS.currentRow()     // 현재 행까지
    )
    .`as`("moving_sum_3")

// 또는 간편 표기
avg(SALES.AMOUNT)
    .over()
    .orderBy(SALES.DATE.asc())
    .rowsBetweenPreceding(2)   // 앞 2행
    .andCurrentRow()
    .`as`("moving_avg_3")
```

### Frame 범위 종류

| jOOQ 메서드 | SQL | 의미 |
|------------|-----|------|
| `.rowsBetweenUnboundedPreceding()` | `ROWS BETWEEN UNBOUNDED PRECEDING` | 처음부터 |
| `.andCurrentRow()` | `AND CURRENT ROW` | 현재 행까지 |
| `.andUnboundedFollowing()` | `AND UNBOUNDED FOLLOWING` | 끝까지 |
| `.rowsBetweenPreceding(n)` | `ROWS BETWEEN n PRECEDING` | n행 이전부터 |
| `.andFollowing(n)` | `AND n FOLLOWING` | n행 이후까지 |

---

## 실전 예제: 사용자 주문 순위 리포트

```kotlin
// 사용자별, 금액 순위 + 누적 비율 한번에 조회
data class OrderRankReport(
    val userId: Long,
    val orderId: Long,
    val amount: BigDecimal,
    val rankInUser: Int,        // 사용자 내 순위
    val totalOrders: Int,       // 사용자 총 주문 수
    val cumulativeAmount: BigDecimal  // 누적 금액
)

fun getOrderRankReport(): List<OrderRankReport> {
    return dsl
        .select(
            ORDERS.USER_ID,
            ORDERS.ID,
            ORDERS.AMOUNT,
            rank()
                .over()
                .partitionBy(ORDERS.USER_ID)
                .orderBy(ORDERS.AMOUNT.desc())
                .`as`("rank_in_user"),
            count()
                .over()
                .partitionBy(ORDERS.USER_ID)
                .`as`("total_orders"),
            sum(ORDERS.AMOUNT)
                .over()
                .partitionBy(ORDERS.USER_ID)
                .orderBy(ORDERS.CREATED_AT.asc())
                .rowsBetweenUnboundedPreceding()
                .andCurrentRow()
                .`as`("cumulative_amount")
        )
        .from(ORDERS)
        .orderBy(ORDERS.USER_ID, ORDERS.AMOUNT.desc())
        .fetchInto(OrderRankReport::class.java)
}
```

---

## NTILE() - 그룹 분할

데이터를 N개의 동일한 크기 그룹으로 나눕니다.

```kotlin
// 점수 기준으로 4분위 구분 (1=상위 25%, 4=하위 25%)
val result = dsl
    .select(
        USERS.NAME,
        USERS.SCORE,
        ntile(4).over().orderBy(USERS.SCORE.desc()).`as`("quartile")
    )
    .from(USERS)
    .fetch()

// quartile=1 이면 상위 25%
val topQuartile = result.filter { it.get("quartile", Int::class.java) == 1 }
```

---

> **참고**: Window Functions는 `WHERE`나 `GROUP BY` 이후에 계산됩니다.
> Window Function의 결과로 필터링하려면 서브쿼리나 CTE로 감싸야 합니다.
>
> ```kotlin
> // Window Function 결과로 필터링 (서브쿼리 사용)
> val ranked = dsl
>     .select(
>         ORDERS.USER_ID,
>         ORDERS.ID,
>         ORDERS.AMOUNT,
>         rank().over().partitionBy(ORDERS.USER_ID)
>             .orderBy(ORDERS.AMOUNT.desc()).`as`("rnk")
>     )
>     .from(ORDERS)
>     .asTable("ranked_orders")
>
> // 사용자별 top 3 주문만 조회
> dsl.select()
>     .from(ranked)
>     .where(ranked.field("rnk", Int::class.java)!!.le(3))
>     .fetch()
> ```
