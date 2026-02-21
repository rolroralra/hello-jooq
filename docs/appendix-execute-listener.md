# 부록: ExecuteListener - 쿼리 실행 생명주기 훅

> 이 문서는 참고용입니다. jOOQ가 SQL을 실행하는 각 단계에 끼어들어 로깅, 성능 측정, 감사 등을 구현하는 방법을 다룹니다.

---

## ExecuteListener란?

jOOQ는 SQL 쿼리를 실행할 때 내부적으로 여러 단계를 거칩니다.
**SQL 렌더링 → PreparedStatement 준비 → 파라미터 바인딩 → 실행 → 결과 패치**

`ExecuteListener`는 이 각 단계의 전후에 호출되는 **콜백 인터페이스**입니다.
데이터베이스와 관련된 횡단 관심사(cross-cutting concerns)를 구현할 때 사용합니다.

```
쿼리 실행 흐름:

start()
  → renderStart() → [SQL 렌더링] → renderEnd()
  → prepareStart() → [PreparedStatement 생성] → prepareEnd()
  → bindStart() → [파라미터 바인딩] → bindEnd()
  → executeStart() → [DB 실행] → executeEnd()
  → fetchStart()
      → resultStart() → [ResultSet 패치] → resultEnd()
      → (행마다) resultFetched()
  → fetchEnd()
end()
```

예외 발생 시 → `exception()` / SQL 경고 발생 시 → `warning()`

---

## 콜백 메서드 목록

| 메서드 | 호출 시점 |
|--------|----------|
| `start(ctx)` | 쿼리 실행 시작 직전 |
| `renderStart(ctx)` | SQL 문자열 렌더링 시작 전 |
| `renderEnd(ctx)` | SQL 문자열 렌더링 완료 후 |
| `prepareStart(ctx)` | `Connection.prepareStatement()` 호출 전 |
| `prepareEnd(ctx)` | `Connection.prepareStatement()` 완료 후 |
| `bindStart(ctx)` | 파라미터 바인딩 시작 전 |
| `bindEnd(ctx)` | 파라미터 바인딩 완료 후 |
| `executeStart(ctx)` | `Statement.execute()` 호출 전 |
| `executeEnd(ctx)` | `Statement.execute()` 완료 후 |
| `fetchStart(ctx)` | ResultSet 패치 시작 전 |
| `resultStart(ctx)` | Result 객체 구성 시작 전 |
| `resultFetched(ctx)` | 개별 Record 하나가 패치될 때마다 |
| `resultEnd(ctx)` | Result 객체 구성 완료 후 |
| `fetchEnd(ctx)` | ResultSet 패치 완료 후 |
| `end(ctx)` | 쿼리 실행 완전히 종료 후 |
| `exception(ctx)` | 예외 발생 시 |
| `warning(ctx)` | SQL Warning 발생 시 |

---

## ExecuteContext

모든 콜백 메서드는 `ExecuteContext`를 파라미터로 받습니다.
실행 중인 쿼리에 대한 정보를 읽거나, 일부 값을 변경할 수 있습니다.

```kotlin
interface ExecuteContext {
    fun dsl(): DSLContext           // 현재 DSLContext
    fun configuration(): Configuration
    fun settings(): Settings

    fun type(): ExecuteType         // READ, WRITE, DDL, BATCH, ROUTINE, OTHER

    fun query(): Query?             // 실행 중인 Query 객체
    fun sql(): String?              // 렌더링된 SQL 문자열
    fun sql(sql: String)            // SQL을 수동으로 변경 (renderEnd 이후)

    fun statement(): PreparedStatement?  // PreparedStatement 객체
    fun resultSet(): ResultSet?          // ResultSet 객체

    fun record(): Record?           // 마지막으로 패치된 Record
    fun result(): Result<*>?        // 마지막으로 패치된 Result

    fun exception(): RuntimeException?          // 발생한 예외
    fun exception(e: RuntimeException)          // 예외를 다른 것으로 교체
    fun sqlWarning(): SQLWarning?               // SQL 경고
}
```

### ExecuteType

`ctx.type()`으로 현재 실행 유형을 구분할 수 있습니다:

| ExecuteType | 설명 |
|-------------|------|
| `READ` | SELECT 쿼리 |
| `WRITE` | INSERT, UPDATE, DELETE |
| `DDL` | CREATE, DROP, ALTER |
| `BATCH` | 배치 실행 |
| `ROUTINE` | 저장 프로시저, 함수 |
| `OTHER` | 그 외 |

---

## DefaultExecuteListener - 기본 구현체

`ExecuteListener`는 메서드가 많으므로, 직접 구현하면 필요 없는 메서드도 모두 override해야 합니다.
`DefaultExecuteListener`를 상속하면 필요한 메서드만 override할 수 있습니다.

```kotlin
// DefaultExecuteListener: 모든 메서드가 빈 구현(no-op)
// 필요한 메서드만 override하면 됩니다
class MyListener : DefaultExecuteListener() {
    override fun executeStart(ctx: ExecuteContext) {
        // 실행 전 처리
    }

    override fun executeEnd(ctx: ExecuteContext) {
        // 실행 후 처리
    }
}
```

---

## 등록 방법

### Spring Boot에서 등록

```kotlin
@Configuration
class JooqConfig(private val dataSource: DataSource) {

    @Bean
    fun dslContext(): DSLContext {
        val configuration = DefaultConfiguration()
            .set(dataSource)
            .set(SQLDialect.MYSQL)
            .set(SlowQueryListener())       // 리스너 등록
            .set(AuditExecuteListener())    // 여러 개 등록 가능

        return DSL.using(configuration)
    }
}
```

`ExecuteListenerProvider`를 빈으로 등록하면 Spring Boot가 자동으로 적용합니다:

```kotlin
@Bean
fun slowQueryListenerProvider(): ExecuteListenerProvider {
    return DefaultExecuteListenerProvider(SlowQueryListener())
}
```

### DSLContext 생성 시 직접 등록

```kotlin
val dsl = DSL.using(
    DefaultConfiguration()
        .set(dataSource)
        .set(SQLDialect.MYSQL)
        .set(DefaultExecuteListenerProvider(SlowQueryListener()))
)
```

---

## 실전 예제

### 1. 슬로우 쿼리 감지 (성능 모니터링)

```kotlin
class SlowQueryListener(
    private val thresholdMs: Long = 1000L
) : DefaultExecuteListener() {

    private val startTime = ThreadLocal<Long>()

    override fun executeStart(ctx: ExecuteContext) {
        startTime.set(System.currentTimeMillis())
    }

    override fun executeEnd(ctx: ExecuteContext) {
        val elapsed = System.currentTimeMillis() - startTime.get()
        startTime.remove()

        if (elapsed > thresholdMs) {
            log.warn("슬로우 쿼리 감지 (${elapsed}ms):\n${ctx.sql()}")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SlowQueryListener::class.java)
    }
}
```

### 2. SQL 로깅

```kotlin
class SqlLoggingListener : DefaultExecuteListener() {

    override fun renderEnd(ctx: ExecuteContext) {
        // 렌더링된 SQL 출력 (바인딩 전)
        log.debug("SQL: {}", ctx.sql())
    }

    override fun executeEnd(ctx: ExecuteContext) {
        // 실행 타입 구분
        when (ctx.type()) {
            ExecuteType.READ  -> log.debug("SELECT 실행 완료")
            ExecuteType.WRITE -> log.info("DML 실행 완료: {}", ctx.sql())
            else              -> {}
        }
    }

    override fun exception(ctx: ExecuteContext) {
        log.error("쿼리 실행 실패: {}", ctx.sql(), ctx.exception())
    }

    companion object {
        private val log = LoggerFactory.getLogger(SqlLoggingListener::class.java)
    }
}
```

### 3. 감사 로그 (Audit Log) - WRITE 쿼리 기록

```kotlin
class AuditExecuteListener(
    private val auditRepository: AuditRepository
) : DefaultExecuteListener() {

    override fun executeEnd(ctx: ExecuteContext) {
        if (ctx.type() == ExecuteType.WRITE) {
            auditRepository.save(
                AuditLog(
                    sql       = ctx.sql() ?: "",
                    executedAt = LocalDateTime.now()
                )
            )
        }
    }
}
```

### 4. 예외 변환 - DataAccessException을 도메인 예외로

```kotlin
class ExceptionTranslationListener : DefaultExecuteListener() {

    override fun exception(ctx: ExecuteContext) {
        val cause = ctx.exception()

        if (cause is DataAccessException) {
            val sqlState = cause.sqlState()

            // MySQL 중복 키 오류 (23000 계열)
            if (sqlState?.startsWith("23") == true) {
                ctx.exception(DuplicateResourceException("이미 존재하는 데이터입니다.", cause))
            }
        }
    }
}

class DuplicateResourceException(message: String, cause: Throwable) :
    RuntimeException(message, cause)
```

### 5. 쿼리 카운터 (테스트에서 N+1 감지)

```kotlin
class QueryCountListener : DefaultExecuteListener() {

    override fun executeStart(ctx: ExecuteContext) {
        counter.incrementAndGet()
    }

    companion object {
        private val counter = AtomicInteger(0)

        fun getCount(): Int = counter.get()
        fun reset() = counter.set(0)
    }
}

// 테스트에서 사용 예
@Test
fun `N+1 문제가 없어야 한다`() {
    QueryCountListener.reset()

    val result = userService.findAllWithOrders()  // 쿼리 실행

    assertThat(QueryCountListener.getCount())
        .isLessThanOrEqualTo(2)  // 예상 쿼리 수
}
```

---

## 여러 리스너 등록 시 실행 순서

리스너는 등록된 순서대로 실행됩니다.
`start()`는 등록 순서, `end()`는 역순으로 호출됩니다 (스택 구조).

```kotlin
// 등록 순서: [LoggingListener, SlowQueryListener]
DefaultConfiguration()
    .set(DefaultExecuteListenerProvider(LoggingListener()))
    .set(DefaultExecuteListenerProvider(SlowQueryListener()))

// 실행 순서:
// start:  LoggingListener → SlowQueryListener
// end:    SlowQueryListener → LoggingListener  (역순)
```

---

## 주의사항

### ThreadLocal 사용 시 반드시 정리

```kotlin
class TimingListener : DefaultExecuteListener() {
    private val startTime = ThreadLocal<Long>()

    override fun executeStart(ctx: ExecuteContext) {
        startTime.set(System.currentTimeMillis())
    }

    override fun executeEnd(ctx: ExecuteContext) {
        val elapsed = System.currentTimeMillis() - startTime.get()
        startTime.remove()  // 반드시 remove() 호출 (메모리 누수 방지)
        log.info("쿼리 실행 시간: ${elapsed}ms")
    }

    override fun exception(ctx: ExecuteContext) {
        startTime.remove()  // 예외 발생 시에도 정리
    }
}
```

### 리스너 내부에서 DB 쿼리 실행 주의

리스너 콜백 안에서 `dsl.selectFrom(...)` 같은 쿼리를 실행하면
해당 쿼리도 같은 리스너를 다시 타게 됩니다 (무한 루프 가능성).
반드시 별도의 `DSLContext` 인스턴스나 플래그로 재진입을 방지하세요.

```kotlin
class AuditListener : DefaultExecuteListener() {
    private val isAuditing = ThreadLocal.withInitial { false }

    override fun executeEnd(ctx: ExecuteContext) {
        if (isAuditing.get()) return  // 재진입 방지

        if (ctx.type() == ExecuteType.WRITE) {
            isAuditing.set(true)
            try {
                // 감사 로그 저장 (JDBC 직접 사용 권장)
            } finally {
                isAuditing.set(false)
            }
        }
    }
}
```

---

## 요약

| 목적 | 사용 메서드 |
|------|------------|
| SQL 로깅 | `renderEnd()` |
| 실행 시간 측정 | `executeStart()` + `executeEnd()` |
| 슬로우 쿼리 감지 | `executeStart()` + `executeEnd()` |
| DML 감사 로그 | `executeEnd()` (type == WRITE 필터) |
| 예외 변환 | `exception()` |
| SQL Warning 처리 | `warning()` |
| 테스트용 쿼리 카운트 | `executeStart()` |

---

> **참고**: jOOQ는 `ExecuteListener` 외에도 `VisitListener`(SQL 렌더링 훅), `RecordListener`(Record 생명주기 훅)도 제공합니다.
> 쿼리 실행 전반에 관여하려면 `ExecuteListener`, SQL AST 변환이 필요하면 `VisitListener`를 사용하세요.
