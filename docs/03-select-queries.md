# Chapter 3: SELECT 쿼리

## 개요

이 챕터에서는 jOOQ를 사용해 다양한 SELECT 쿼리를 작성하는 방법을 배웁니다.

## 기본 SELECT

### 전체 컬럼 조회

```kotlin
// SQL: SELECT * FROM users
val users = dsl.select()
    .from(USERS)
    .fetch()

// 또는 더 간단하게
val users = dsl.selectFrom(USERS).fetch()
```

### 특정 컬럼 조회

```kotlin
// SQL: SELECT id, name, email FROM users
val result = dsl.select(USERS.ID, USERS.NAME, USERS.EMAIL)
    .from(USERS)
    .fetch()

// 결과 접근
result.forEach { record ->
    val id = record[USERS.ID]
    val name = record[USERS.NAME]
    val email = record[USERS.EMAIL]
    println("$id: $name ($email)")
}
```

## WHERE 조건

### 기본 조건

```kotlin
// SQL: SELECT * FROM users WHERE id = 1
dsl.selectFrom(USERS)
    .where(USERS.ID.eq(1))
    .fetchOne()

// SQL: SELECT * FROM users WHERE name = 'John'
dsl.selectFrom(USERS)
    .where(USERS.NAME.eq("John"))
    .fetch()
```

### 비교 연산자

```kotlin
// eq: 같음 (=)
USERS.ID.eq(1)

// ne: 같지 않음 (<>)
USERS.STATUS.ne("DELETED")

// lt: 작음 (<)
USERS.AGE.lt(30)

// le: 작거나 같음 (<=)
USERS.AGE.le(30)

// gt: 큼 (>)
USERS.CREATED_AT.gt(LocalDateTime.now().minusDays(7))

// ge: 크거나 같음 (>=)
USERS.SCORE.ge(80)
```

### NULL 체크

```kotlin
// SQL: SELECT * FROM users WHERE deleted_at IS NULL
dsl.selectFrom(USERS)
    .where(USERS.DELETED_AT.isNull)
    .fetch()

// SQL: SELECT * FROM users WHERE deleted_at IS NOT NULL
dsl.selectFrom(USERS)
    .where(USERS.DELETED_AT.isNotNull)
    .fetch()
```

### LIKE 검색

```kotlin
// SQL: SELECT * FROM users WHERE name LIKE '%John%'
dsl.selectFrom(USERS)
    .where(USERS.NAME.like("%John%"))
    .fetch()

// SQL: SELECT * FROM users WHERE name LIKE 'John%'
dsl.selectFrom(USERS)
    .where(USERS.NAME.startsWith("John"))
    .fetch()

// SQL: SELECT * FROM users WHERE name LIKE '%son'
dsl.selectFrom(USERS)
    .where(USERS.NAME.endsWith("son"))
    .fetch()

// 대소문자 무시
dsl.selectFrom(USERS)
    .where(USERS.NAME.likeIgnoreCase("%john%"))
    .fetch()
```

### IN 절

```kotlin
// SQL: SELECT * FROM users WHERE id IN (1, 2, 3)
dsl.selectFrom(USERS)
    .where(USERS.ID.`in`(1, 2, 3))
    .fetch()

// 리스트 사용
val ids = listOf(1, 2, 3)
dsl.selectFrom(USERS)
    .where(USERS.ID.`in`(ids))
    .fetch()

// NOT IN
dsl.selectFrom(USERS)
    .where(USERS.STATUS.notIn("DELETED", "BANNED"))
    .fetch()
```

### BETWEEN

```kotlin
// SQL: SELECT * FROM users WHERE age BETWEEN 20 AND 30
dsl.selectFrom(USERS)
    .where(USERS.AGE.between(20, 30))
    .fetch()
```

## 논리 연산자 (AND, OR)

### AND 조건

```kotlin
// SQL: SELECT * FROM users WHERE status = 'ACTIVE' AND age >= 18
dsl.selectFrom(USERS)
    .where(USERS.STATUS.eq("ACTIVE"))
    .and(USERS.AGE.ge(18))
    .fetch()

// 여러 조건을 한 번에
dsl.selectFrom(USERS)
    .where(
        USERS.STATUS.eq("ACTIVE"),
        USERS.AGE.ge(18),
        USERS.EMAIL.isNotNull
    )
    .fetch()
```

### OR 조건

```kotlin
// SQL: SELECT * FROM users WHERE status = 'ACTIVE' OR status = 'PENDING'
dsl.selectFrom(USERS)
    .where(USERS.STATUS.eq("ACTIVE"))
    .or(USERS.STATUS.eq("PENDING"))
    .fetch()

// or() 메서드 사용
dsl.selectFrom(USERS)
    .where(USERS.STATUS.eq("ACTIVE").or(USERS.STATUS.eq("PENDING")))
    .fetch()
```

### 복합 조건

```kotlin
// SQL: SELECT * FROM users
//      WHERE (status = 'ACTIVE' OR status = 'PENDING')
//      AND age >= 18
dsl.selectFrom(USERS)
    .where(
        USERS.STATUS.eq("ACTIVE").or(USERS.STATUS.eq("PENDING"))
    )
    .and(USERS.AGE.ge(18))
    .fetch()
```

## 정렬 (ORDER BY)

```kotlin
// SQL: SELECT * FROM users ORDER BY created_at DESC
dsl.selectFrom(USERS)
    .orderBy(USERS.CREATED_AT.desc())
    .fetch()

// 여러 컬럼 정렬
// SQL: SELECT * FROM users ORDER BY status ASC, created_at DESC
dsl.selectFrom(USERS)
    .orderBy(USERS.STATUS.asc(), USERS.CREATED_AT.desc())
    .fetch()

// NULL 처리
dsl.selectFrom(USERS)
    .orderBy(USERS.NAME.asc().nullsLast())
    .fetch()
```

## 페이징 (LIMIT, OFFSET)

```kotlin
// SQL: SELECT * FROM users LIMIT 10
dsl.selectFrom(USERS)
    .limit(10)
    .fetch()

// SQL: SELECT * FROM users LIMIT 10 OFFSET 20
dsl.selectFrom(USERS)
    .limit(10)
    .offset(20)
    .fetch()

// 페이지 기반 조회
fun findUsersByPage(page: Int, size: Int): List<UsersRecord> {
    return dsl.selectFrom(USERS)
        .orderBy(USERS.ID)
        .limit(size)
        .offset((page - 1) * size)
        .fetch()
}
```

## 집계 함수

```kotlin
import org.jooq.impl.DSL.*

// COUNT
// SQL: SELECT COUNT(*) FROM users
val count = dsl.selectCount()
    .from(USERS)
    .fetchOne(0, Int::class.java)

// 조건과 함께
val activeCount = dsl.selectCount()
    .from(USERS)
    .where(USERS.STATUS.eq("ACTIVE"))
    .fetchOne(0, Int::class.java)

// SUM
// SQL: SELECT SUM(amount) FROM orders
val totalAmount = dsl.select(sum(ORDERS.AMOUNT))
    .from(ORDERS)
    .fetchOne(0, BigDecimal::class.java)

// AVG
val avgAge = dsl.select(avg(USERS.AGE))
    .from(USERS)
    .fetchOne(0, BigDecimal::class.java)

// MAX, MIN
val maxScore = dsl.select(max(USERS.SCORE))
    .from(USERS)
    .fetchOne(0, Int::class.java)
```

## GROUP BY와 HAVING

```kotlin
// SQL: SELECT status, COUNT(*) as count
//      FROM users
//      GROUP BY status
val statusCounts = dsl.select(USERS.STATUS, count())
    .from(USERS)
    .groupBy(USERS.STATUS)
    .fetch()

// HAVING 조건
// SQL: SELECT status, COUNT(*) as count
//      FROM users
//      GROUP BY status
//      HAVING COUNT(*) > 10
val popularStatuses = dsl.select(USERS.STATUS, count().`as`("count"))
    .from(USERS)
    .groupBy(USERS.STATUS)
    .having(count().gt(10))
    .fetch()
```

## DISTINCT

```kotlin
// SQL: SELECT DISTINCT status FROM users
dsl.selectDistinct(USERS.STATUS)
    .from(USERS)
    .fetch()
```

## 결과 매핑

### Record에서 값 추출

```kotlin
val record = dsl.selectFrom(USERS)
    .where(USERS.ID.eq(1))
    .fetchOne()

// 인덱스로 접근
val id = record?.get(0)

// 필드로 접근 (권장)
val name = record?.get(USERS.NAME)

// into() 메서드로 POJO 변환
data class UserDto(val id: Int, val name: String, val email: String)

val user = dsl.select(USERS.ID, USERS.NAME, USERS.EMAIL)
    .from(USERS)
    .where(USERS.ID.eq(1))
    .fetchOneInto(UserDto::class.java)
```

### 리스트로 매핑

```kotlin
// Record 리스트
val records: List<UsersRecord> = dsl.selectFrom(USERS).fetch()

// POJO 리스트
val users: List<UserDto> = dsl.selectFrom(USERS)
    .fetchInto(UserDto::class.java)

// 특정 컬럼만 리스트로
val names: List<String> = dsl.select(USERS.NAME)
    .from(USERS)
    .fetch(USERS.NAME)
```

## 조건부 쿼리 (동적 쿼리)

```kotlin
fun searchUsers(
    name: String? = null,
    status: String? = null,
    minAge: Int? = null
): List<UsersRecord> {
    return dsl.selectFrom(USERS)
        .where(
            // 조건이 있을 때만 추가
            name?.let { USERS.NAME.like("%$it%") },
            status?.let { USERS.STATUS.eq(it) },
            minAge?.let { USERS.AGE.ge(it) }
        )
        .fetch()
}

// DSL.noCondition() 사용
fun searchUsersV2(
    name: String? = null,
    status: String? = null
): List<UsersRecord> {
    var condition = DSL.noCondition()

    name?.let { condition = condition.and(USERS.NAME.like("%$it%")) }
    status?.let { condition = condition.and(USERS.STATUS.eq(it)) }

    return dsl.selectFrom(USERS)
        .where(condition)
        .fetch()
}
```

## 다음 단계

다음 챕터에서는 데이터를 조작하는 INSERT, UPDATE, DELETE 쿼리를 알아보겠습니다.

[← Chapter 2: 프로젝트 설정](./02-project-setup.md) | [Chapter 4: INSERT, UPDATE, DELETE →](./04-data-manipulation.md)
