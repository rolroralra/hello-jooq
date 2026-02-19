# 부록: jOOQ 핵심 클래스 개념 정리

> 이 문서는 참고용입니다. jOOQ 코드를 읽다 보면 자주 마주치는 클래스들의 개념을 간단히 정리했습니다.

---

## 전체 구조 한눈에 보기

```
데이터베이스 세계          jOOQ 세계
─────────────────────────────────────────────────
테이블 (table)      →     Table<R>
컬럼 (column)       →     Field<T>
행 묶음 (row tuple) →     Row
한 행 (row)         →     Record
결과셋 (result set) →     Result<R>
WHERE 조건          →     Condition
```

jOOQ는 SQL의 각 구성요소를 Java/Kotlin 객체로 모델링합니다.
쿼리를 작성하는 것이 곧 이 객체들을 조립하는 과정입니다.

---

## Table&lt;R&gt;

**"데이터베이스의 테이블을 나타내는 객체"**

jOOQ 코드 생성기가 `schema.tables` 패키지 아래에 자동으로 만들어 줍니다.
`FROM`, `JOIN` 절에서 사용합니다.

```kotlin
// 생성된 클래스 예시 (직접 작성하는 코드가 아님)
// public class Users extends TableImpl<UsersRecord> { ... }

// 실제 사용 - 보통 static import로 간결하게 씁니다
import com.example.hellojooq.jooq.tables.Users.USERS

dsl.selectFrom(USERS)   // FROM users
   .fetch()
```

타입 파라미터 `R`은 이 테이블에 대응하는 **Record 타입**입니다.
예: `Table<UsersRecord>` → 이 테이블을 조회하면 `UsersRecord`가 나옵니다.

---

## Field&lt;T&gt;

**"테이블의 컬럼(열)을 나타내는 객체"**

`Table` 안에 컬럼 하나하나가 `Field`로 정의되어 있습니다.
`SELECT`, `WHERE`, `ORDER BY` 절에서 사용합니다.

```kotlin
// 컬럼 접근
USERS.ID        // Field<Long>
USERS.NAME      // Field<String>
USERS.EMAIL     // Field<String>
USERS.ACTIVE    // Field<Boolean>

// WHERE 절에서 Field로 조건을 만들 수 있습니다
dsl.selectFrom(USERS)
   .where(USERS.ACTIVE.isTrue())          // Field -> Condition
   .orderBy(USERS.NAME.asc())             // Field -> SortField
   .fetch()
```

타입 파라미터 `T`는 **컬럼의 Java 타입**입니다.
`USERS.ID`는 `Field<Long>`이므로, `.eq("hello")` 같이 타입이 맞지 않으면 컴파일 에러가 납니다.

---

## Row

**"여러 컬럼을 하나로 묶은 값 집합"**

단일 컬럼이 아닌 **복수 컬럼을 동시에 비교**할 때 사용합니다.
SQL의 `(col1, col2) = (val1, val2)` 구문에 해당합니다.

```kotlin
// (user_id, product_id) 쌍으로 조건을 걸 때
dsl.selectFrom(ORDER_ITEMS)
   .where(
       row(ORDER_ITEMS.USER_ID, ORDER_ITEMS.PRODUCT_ID)
           .eq(row(1L, 42L))
   )
   .fetch()

// IN 절과 함께 - 여러 쌍을 한번에 비교
dsl.selectFrom(ORDER_ITEMS)
   .where(
       row(ORDER_ITEMS.USER_ID, ORDER_ITEMS.PRODUCT_ID)
           .`in`(
               row(1L, 42L),
               row(2L, 99L)
           )
   )
   .fetch()
```

`Row1`, `Row2`, ... `Row22`처럼 컬럼 개수별로 타입이 나뉩니다.
일반적인 단일 컬럼 비교에는 `Field`를 직접 씁니다.

---

## Record

**"쿼리 결과의 한 행(row)을 나타내는 객체"**

`fetch()`로 받은 결과의 행 하나가 `Record`입니다.
컬럼 이름이나 인덱스로 값을 꺼낼 수 있습니다.

```kotlin
// 제네릭 Record - 어떤 쿼리에서든 사용 가능
val record: Record = dsl.select(USERS.NAME, USERS.EMAIL)
    .from(USERS)
    .where(USERS.ID.eq(1L))
    .fetchOne()

val name = record.get(USERS.NAME)   // String
val email = record.get(USERS.EMAIL) // String

// 테이블 전용 Record (코드 생성 시 자동 생성)
// 각 컬럼에 대한 getter 메서드가 있어서 더 편리합니다
val userRecord: UsersRecord = dsl.fetchOne(USERS, USERS.ID.eq(1L))

val name = userRecord.name   // getter 자동 생성
val email = userRecord.email
```

### Record의 종류

| 타입 | 설명 | 예시 |
|------|------|------|
| `Record` | 가장 기본형, 컬럼 수 제한 없음 | `SELECT *` 결과 |
| `Record1<T1>` ~ `Record22<...>` | 컬럼 수가 고정된 경우 타입 안전 | `SELECT id, name` → `Record2<Long, String>` |
| `TableRecord<T>` | 특정 테이블에 종속된 Record | `UsersRecord`, `ProductsRecord` |
| `UpdatableRecord<T>` | DB에 직접 저장/수정/삭제 가능 | `userRecord.store()` |

---

## Result&lt;R&gt;

**"여러 Record를 담은 결과 목록"**

`List<R>`을 구현하므로 일반 컬렉션처럼 사용할 수 있습니다.
jOOQ가 제공하는 편의 메서드도 추가로 사용할 수 있습니다.

```kotlin
// fetch() → Result<Record>
val result: Result<UsersRecord> = dsl.selectFrom(USERS).fetch()

// List처럼 사용 가능
result.size
result.isEmpty()
result.forEach { record -> println(record.name) }

// jOOQ 편의 메서드
val nameList: List<String>      = result.getValues(USERS.NAME)
val nameSet: Set<String>        = result.intoSet(USERS.NAME)
val nameMap: Map<Long, String>  = result.intoMap(USERS.ID, USERS.NAME)
```

### fetch 계열 메서드 비교

| 메서드 | 반환 타입 | 용도 |
|--------|-----------|------|
| `fetch()` | `Result<R>` | 여러 행 조회 |
| `fetchOne()` | `R?` | 정확히 1행 (없으면 null, 2개 이상이면 예외) |
| `fetchAny()` | `R?` | 첫 번째 행 (없으면 null) |
| `fetchSingle()` | `R` | 정확히 1행 (없거나 2개 이상이면 예외) |
| `fetchInto(Class)` | `List<T>` | POJO로 매핑해서 반환 |
| `fetchStream()` | `Stream<R>` | 스트림으로 반환 |

---

## Condition

**"WHERE 절의 조건식을 나타내는 객체"**

`Field`의 비교 메서드를 호출하면 `Condition`이 만들어집니다.
`and()`, `or()`, `not()`으로 조건을 조합할 수 있습니다.

```kotlin
// Field 비교 → Condition 생성
val isActive: Condition    = USERS.ACTIVE.isTrue()
val nameMatch: Condition   = USERS.NAME.like("%kim%")
val recentOrder: Condition = ORDERS.CREATED_AT.greaterThan(LocalDateTime.now().minusDays(7))

// 조건 조합
dsl.selectFrom(USERS)
   .where(
       isActive
           .and(nameMatch)             // AND 조합
           .and(USERS.ID.ge(10L))      // 추가 AND
   )
   .fetch()

// OR 조합
dsl.selectFrom(PRODUCTS)
   .where(
       PRODUCTS.PRICE.lt(BigDecimal("10000"))
           .or(PRODUCTS.STOCK.eq(0))
   )
   .fetch()

// 동적 조건 조합 (조건이 null일 수 있을 때 유용)
val conditions = mutableListOf<Condition>()
if (keyword != null)  conditions.add(PRODUCTS.NAME.containsIgnoreCase(keyword))
if (minPrice != null) conditions.add(PRODUCTS.PRICE.ge(minPrice))

dsl.selectFrom(PRODUCTS)
   .where(DSL.and(conditions))   // 빈 리스트면 true (전체 조회)
   .fetch()
```

### 자주 쓰는 Condition 생성 메서드

| 메서드 | SQL 대응 | 예시 |
|--------|----------|------|
| `.eq(value)` | `= ?` | `USERS.ID.eq(1L)` |
| `.ne(value)` | `<> ?` | `USERS.ID.ne(1L)` |
| `.gt(value)` | `> ?` | `PRICE.gt(1000)` |
| `.ge(value)` | `>= ?` | `PRICE.ge(1000)` |
| `.lt(value)` | `< ?` | `PRICE.lt(1000)` |
| `.le(value)` | `<= ?` | `PRICE.le(1000)` |
| `.isNull()` | `IS NULL` | `USERS.DELETED_AT.isNull()` |
| `.isNotNull()` | `IS NOT NULL` | `USERS.EMAIL.isNotNull()` |
| `.like(pattern)` | `LIKE ?` | `NAME.like("%kim%")` |
| `.in(values)` | `IN (...)` | `STATUS.in("PENDING", "CONFIRMED")` |
| `.between(a, b)` | `BETWEEN ? AND ?` | `PRICE.between(1000, 5000)` |

---

## 전체 흐름으로 이해하기

```kotlin
// 아래 쿼리에서 각 클래스가 어떤 역할을 하는지 살펴보세요
val result: Result<Record2<String, BigDecimal>> =
    dsl
        .select(
            USERS.NAME,          // Field<String>
            ORDERS.TOTAL_AMOUNT  // Field<BigDecimal>
        )
        .from(USERS)             // Table<UsersRecord>
        .join(ORDERS)            // Table<OrdersRecord>
        .on(USERS.ID.eq(ORDERS.USER_ID))   // Condition
        .where(
            ORDERS.STATUS.eq("COMPLETED")  // Condition
                .and(USERS.ACTIVE.isTrue())
        )
        .fetch()                 // Result<Record2<String, BigDecimal>>

// result 순회 → 각 요소는 Record2<String, BigDecimal>
result.forEach { record ->
    val userName: String        = record.value1()
    val totalAmount: BigDecimal = record.value2()
}
```

---

> **참고**: 이 문서는 개념 이해를 돕기 위한 것입니다.
> 실제 코드 예제는 [Chapter 3: SELECT 쿼리](./03-select-queries.md)와 [Chapter 5: JOIN과 서브쿼리](./05-joins-and-subqueries.md)를 참고하세요.
