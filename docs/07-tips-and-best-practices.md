# Chapter 7: 팁과 모범 사례

## 개요

이 챕터에서는 jOOQ를 실무에서 효과적으로 사용하기 위한 팁과 모범 사례를 다룹니다.

## 성능 최적화

### 1. 필요한 컬럼만 조회하기

```kotlin
// Bad: 모든 컬럼 조회
dsl.selectFrom(USERS).fetch()

// Good: 필요한 컬럼만 조회
dsl.select(USERS.ID, USERS.NAME, USERS.EMAIL)
    .from(USERS)
    .fetch()
```

### 2. fetchLazy() 사용하기

대량의 데이터를 처리할 때는 `fetchLazy()`를 사용하세요.

```kotlin
// 한 번에 모든 데이터를 메모리에 로드
val allUsers = dsl.selectFrom(USERS).fetch()  // 메모리 부담

// Cursor 사용으로 메모리 효율적 처리
dsl.selectFrom(USERS).fetchLazy().use { cursor ->
    cursor.forEach { record ->
        processRecord(record)
    }
}
```

### 3. 배치 처리 활용

```kotlin
// Bad: 반복문에서 개별 INSERT
users.forEach { user ->
    dsl.insertInto(USERS)
        .set(USERS.NAME, user.name)
        .execute()
}

// Good: 배치 INSERT
val records = users.map { user ->
    dsl.newRecord(USERS).apply {
        name = user.name
        email = user.email
    }
}
dsl.batchInsert(records).execute()
```

### 4. 인덱스 활용 확인

```kotlin
// EXPLAIN 사용해서 쿼리 플랜 확인
val explain = dsl.explain(
    dsl.selectFrom(USERS)
        .where(USERS.EMAIL.eq("test@example.com"))
).fetch()

println(explain)
```

## 코드 구조화

### 1. Repository 계층 분리

```kotlin
// UserRepository.kt
@Repository
class UserRepository(private val dsl: DSLContext) {

    fun findById(id: Int): User? =
        dsl.selectFrom(USERS)
            .where(USERS.ID.eq(id))
            .fetchOneInto(User::class.java)

    fun findByStatus(status: String): List<User> =
        dsl.selectFrom(USERS)
            .where(USERS.STATUS.eq(status))
            .fetchInto(User::class.java)
}

// UserService.kt
@Service
class UserService(
    private val userRepository: UserRepository
) {
    fun getActiveUsers(): List<User> =
        userRepository.findByStatus("ACTIVE")
}
```

### 2. 쿼리 빌더 패턴

복잡한 동적 쿼리는 별도 클래스로 분리하세요.

```kotlin
class UserSearchQuery(
    private val dsl: DSLContext
) {
    private var conditions: Condition = DSL.noCondition()
    private var orderFields: MutableList<SortField<*>> = mutableListOf()

    fun withName(name: String?): UserSearchQuery {
        name?.let { conditions = conditions.and(USERS.NAME.like("%$it%")) }
        return this
    }

    fun withStatus(status: String?): UserSearchQuery {
        status?.let { conditions = conditions.and(USERS.STATUS.eq(it)) }
        return this
    }

    fun withAgeRange(min: Int?, max: Int?): UserSearchQuery {
        min?.let { conditions = conditions.and(USERS.AGE.ge(it)) }
        max?.let { conditions = conditions.and(USERS.AGE.le(it)) }
        return this
    }

    fun orderBy(field: String, ascending: Boolean = true): UserSearchQuery {
        val sortField = when (field) {
            "name" -> if (ascending) USERS.NAME.asc() else USERS.NAME.desc()
            "createdAt" -> if (ascending) USERS.CREATED_AT.asc() else USERS.CREATED_AT.desc()
            else -> USERS.ID.asc()
        }
        orderFields.add(sortField)
        return this
    }

    fun fetch(): List<User> {
        return dsl.selectFrom(USERS)
            .where(conditions)
            .orderBy(orderFields)
            .fetchInto(User::class.java)
    }
}

// 사용
val users = UserSearchQuery(dsl)
    .withName("John")
    .withStatus("ACTIVE")
    .withAgeRange(min = 20, max = 40)
    .orderBy("createdAt", ascending = false)
    .fetch()
```

### 3. 공통 조건 재사용

```kotlin
object UserConditions {
    val isActive: Condition = USERS.STATUS.eq("ACTIVE")
    val isNotDeleted: Condition = USERS.DELETED_AT.isNull

    fun isOlderThan(age: Int): Condition = USERS.AGE.gt(age)
    fun belongsToTeam(teamId: Int): Condition = USERS.TEAM_ID.eq(teamId)
}

// 사용
dsl.selectFrom(USERS)
    .where(UserConditions.isActive)
    .and(UserConditions.isNotDeleted)
    .and(UserConditions.isOlderThan(18))
    .fetch()
```

## 에러 처리

### 1. 결과 없음 처리

```kotlin
// fetchOne()은 null 또는 예외 발생 가능
fun findById(id: Int): User {
    return dsl.selectFrom(USERS)
        .where(USERS.ID.eq(id))
        .fetchOneInto(User::class.java)
        ?: throw UserNotFoundException("User not found: $id")
}

// fetchOptional() 사용
fun findByIdOptional(id: Int): Optional<User> {
    return dsl.selectFrom(USERS)
        .where(USERS.ID.eq(id))
        .fetchOptionalInto(User::class.java)
}
```

### 2. 중복 결과 처리

```kotlin
// fetchOne()은 결과가 2개 이상이면 TooManyRowsException 발생
// 이를 방지하려면 limit(1) 사용
fun findFirstByEmail(email: String): User? {
    return dsl.selectFrom(USERS)
        .where(USERS.EMAIL.eq(email))
        .limit(1)  // 첫 번째 결과만
        .fetchOneInto(User::class.java)
}
```

### 3. 낙관적 락 (Optimistic Locking)

```kotlin
// version 컬럼 활용
@Transactional
fun updateWithOptimisticLock(userId: Int, newName: String, expectedVersion: Int) {
    val updated = dsl.update(USERS)
        .set(USERS.NAME, newName)
        .set(USERS.VERSION, expectedVersion + 1)
        .where(USERS.ID.eq(userId))
        .and(USERS.VERSION.eq(expectedVersion))
        .execute()

    if (updated == 0) {
        throw OptimisticLockException("User was modified by another transaction")
    }
}
```

## 디버깅

### 1. SQL 로깅 설정

```yaml
# application.yml
logging:
  level:
    org.jooq.tools.LoggerListener: DEBUG
```

### 2. 쿼리 문자열 확인

```kotlin
val query = dsl.selectFrom(USERS)
    .where(USERS.STATUS.eq("ACTIVE"))

// SQL 문자열 확인
println(query.sql)
// SELECT "public"."users"."id", ... FROM "public"."users" WHERE "public"."users"."status" = ?

// 바인딩 값 포함
println(query.getSQL(ParamType.INLINED))
// SELECT "public"."users"."id", ... FROM "public"."users" WHERE "public"."users"."status" = 'ACTIVE'
```

### 3. ExecuteListener로 쿼리 모니터링

```kotlin
class QueryMonitorListener : DefaultExecuteListener() {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val slowQueryThresholdMs = 1000L

    override fun executeEnd(ctx: ExecuteContext) {
        val executionTime = ctx.executeTime()

        if (executionTime > slowQueryThresholdMs) {
            log.warn("Slow query detected ({}ms): {}", executionTime, ctx.query())
        }
    }
}
```

## 타입 안전성 활용

### 1. Enum 타입 매핑

```kotlin
enum class UserStatus {
    ACTIVE, INACTIVE, DELETED
}

// Converter 정의
class UserStatusConverter : Converter<String, UserStatus> {
    override fun from(dbValue: String?): UserStatus? =
        dbValue?.let { UserStatus.valueOf(it) }

    override fun to(userValue: UserStatus?): String? =
        userValue?.name

    override fun fromType(): Class<String> = String::class.java
    override fun toType(): Class<UserStatus> = UserStatus::class.java
}

// 사용
dsl.selectFrom(USERS)
    .where(USERS.STATUS.eq(UserStatus.ACTIVE.name))
    .fetch()
```

### 2. JSON 타입 다루기

```kotlin
// PostgreSQL의 JSONB 타입
data class UserPreferences(
    val theme: String,
    val language: String,
    val notifications: Boolean
)

// Jackson ObjectMapper 사용
val objectMapper = ObjectMapper()

fun updatePreferences(userId: Int, preferences: UserPreferences) {
    dsl.update(USERS)
        .set(USERS.PREFERENCES, JSONB.valueOf(objectMapper.writeValueAsString(preferences)))
        .where(USERS.ID.eq(userId))
        .execute()
}
```

## 코드 생성 팁

### 1. 테이블/컬럼 제외하기

```kotlin
// build.gradle.kts
jooq {
    configurations {
        create("main") {
            jooqConfiguration.apply {
                generator.apply {
                    database.apply {
                        // 특정 테이블 제외
                        excludes = "flyway_schema_history|temp_.*|log_.*"

                        // 특정 스키마만 포함
                        inputSchema = "public"
                    }
                }
            }
        }
    }
}
```

### 2. 커스텀 타입 매핑

```kotlin
jooq {
    configurations {
        create("main") {
            jooqConfiguration.apply {
                generator.apply {
                    database.apply {
                        forcedTypes.addAll(listOf(
                            org.jooq.meta.jaxb.ForcedType().apply {
                                name = "INSTANT"
                                includeTypes = "TIMESTAMP.*"
                            },
                            org.jooq.meta.jaxb.ForcedType().apply {
                                userType = "com.example.domain.Money"
                                converter = "com.example.jooq.MoneyConverter"
                                includeExpression = ".*\\.price|.*\\.amount"
                            }
                        ))
                    }
                }
            }
        }
    }
}
```

## 자주 하는 실수 피하기

### 1. N+1 문제

```kotlin
// Bad: N+1 쿼리 발생
val orders = dsl.selectFrom(ORDERS).fetch()
orders.forEach { order ->
    val user = dsl.selectFrom(USERS)
        .where(USERS.ID.eq(order.userId))
        .fetchOne()  // 주문 수만큼 쿼리 실행
}

// Good: JOIN 사용
val ordersWithUsers = dsl.select()
    .from(ORDERS)
    .join(USERS).on(ORDERS.USER_ID.eq(USERS.ID))
    .fetch()
```

### 2. 트랜잭션 범위 주의

```kotlin
// Bad: 트랜잭션 없이 여러 작업
fun createOrder(order: Order) {
    dsl.insertInto(ORDERS).set(...).execute()
    // 여기서 예외 발생 시 위 INSERT는 이미 커밋됨
    dsl.insertInto(ORDER_ITEMS).set(...).execute()
}

// Good: 트랜잭션으로 묶기
@Transactional
fun createOrder(order: Order) {
    dsl.insertInto(ORDERS).set(...).execute()
    dsl.insertInto(ORDER_ITEMS).set(...).execute()
    // 하나라도 실패하면 전체 롤백
}
```

### 3. Kotlin에서 예약어 처리

```kotlin
// `in`은 Kotlin 예약어이므로 백틱 사용
dsl.selectFrom(USERS)
    .where(USERS.ID.`in`(1, 2, 3))
    .fetch()

// `as`도 마찬가지
dsl.select(count().`as`("total"))
    .from(USERS)
    .fetch()
```

## 마무리

jOOQ를 사용하면서 가장 중요한 것은:

1. **타입 안전성 활용**: 컴파일 타임에 오류를 잡을 수 있는 장점을 최대한 활용하세요.
2. **SQL 친화적**: jOOQ는 SQL을 추상화하지 않습니다. SQL을 잘 알면 jOOQ도 잘 쓸 수 있습니다.
3. **성능 모니터링**: 실행되는 SQL을 항상 확인하고, 슬로우 쿼리를 모니터링하세요.

[← Chapter 6: Spring Boot 통합](./06-spring-boot-integration.md) | [처음으로 돌아가기 →](./01-introduction.md)
