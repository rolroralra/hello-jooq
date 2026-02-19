# 부록: Plain SQL 템플릿

> 이 문서는 참고용입니다. jOOQ의 DSL로 표현하기 어려운 DB 고유 기능이나 복잡한 표현식을 raw SQL로 작성하는 방법을 다룹니다.

---

## 왜 Plain SQL이 필요한가?

jOOQ의 DSL은 대부분의 SQL을 타입 안전하게 작성할 수 있지만, 아래 경우에는 raw SQL이 필요합니다:

- 특정 DB 고유 함수 (PostgreSQL의 `to_tsvector()`, `array_agg()` 등)
- jOOQ 버전에서 아직 지원하지 않는 SQL 문법
- 레거시 SQL을 점진적으로 jOOQ로 마이그레이션하는 경우

```kotlin
import org.jooq.impl.DSL  // DSL.field(), DSL.condition(), DSL.table() 등
```

> **보안 주의**: Plain SQL을 사용할 때는 **반드시 bind variable(`?`)을 사용**하거나 `DSL.inline()`으로 안전하게 처리하세요.
> 사용자 입력을 직접 문자열로 이어 붙이면 SQL Injection 위험이 있습니다.

---

## DSL.field() - 컬럼 또는 표현식

jOOQ가 모르는 함수나 표현식을 `Field`로 만들 때 사용합니다.

```kotlin
// 기본 사용 - 타입 명시 필수
val field: Field<String> = DSL.field("some_custom_function(column)", String::class.java)

// bind variable 사용 (SQL Injection 방지)
val field: Field<BigDecimal> = DSL.field("my_func({0}, {1})", BigDecimal::class.java,
    USERS.ID,        // {0} 자리에 바인딩
    DSL.inline(100)  // {1} 자리에 바인딩
)
```

### PostgreSQL 전문검색 (Full-Text Search)

```kotlin
// SQL: to_tsvector('korean', title) @@ to_tsquery('korean', ?)
fun searchByFullText(keyword: String): List<ArticlesRecord> {
    val tsVector = DSL.field("to_tsvector('korean', {0})", Boolean::class.java, ARTICLES.TITLE)
    val tsQuery  = DSL.field("to_tsquery('korean', {0})", Boolean::class.java, DSL.inline(keyword))
    val matchExpr = DSL.condition("{0} @@ {1}", tsVector, tsQuery)

    return dsl.selectFrom(ARTICLES)
        .where(matchExpr)
        .fetch()
}
```

### PostgreSQL 배열 함수

```kotlin
// SQL: array_agg(tag ORDER BY tag) AS tags
val tags = DSL.field("array_agg({0} ORDER BY {0})", Array<String>::class.java, ARTICLE_TAGS.TAG)
    .`as`("tags")

val result = dsl
    .select(ARTICLES.ID, ARTICLES.TITLE, tags)
    .from(ARTICLES)
    .leftJoin(ARTICLE_TAGS).on(ARTICLES.ID.eq(ARTICLE_TAGS.ARTICLE_ID))
    .groupBy(ARTICLES.ID, ARTICLES.TITLE)
    .fetch()
```

---

## DSL.condition() - WHERE 조건

jOOQ로 표현하기 어려운 조건식을 `Condition`으로 만들 때 사용합니다.

```kotlin
// 기본 사용
val cond: Condition = DSL.condition("some_function({0}) = true", USERS.ID)

// bind variable 안전하게 사용
val cond: Condition = DSL.condition("{0} @> {1}::jsonb",
    USERS.METADATA,
    DSL.inline("""{"role": "admin"}""")
)
```

### PostgreSQL JSONB 조건

```kotlin
// JSONB 필드에서 특정 키-값 포함 여부
fun findByMetadata(key: String, value: String): List<UsersRecord> {
    val jsonbCond = DSL.condition(
        "{0} @> {1}::jsonb",
        USERS.METADATA,
        DSL.inline("""{"$key": "$value"}""")
    )

    return dsl.selectFrom(USERS)
        .where(jsonbCond)
        .fetch()
}

// JSONB 경로로 값 추출
val role = DSL.field("{0}->>'role'", String::class.java, USERS.METADATA)

dsl.select(USERS.NAME, role.`as`("role"))
    .from(USERS)
    .where(role.eq("admin"))
    .fetch()
```

### PostgreSQL ILIKE (대소문자 무시 패턴 매칭)

```kotlin
// jOOQ의 likeIgnoreCase()로 대체 가능하지만, 필요 시 직접 작성
val cond = DSL.condition("{0} ILIKE {1}", PRODUCTS.NAME, DSL.inline("%macbook%"))
```

---

## DSL.table() - 테이블 또는 뷰

jOOQ 코드 생성 없이 테이블 이름을 직접 참조할 때 사용합니다.

```kotlin
// 테이블 직접 참조
val usersTable: Table<Record> = DSL.table("users")

// 별칭 사용
val u = DSL.table("users").`as`("u")

dsl.select(DSL.field("u.name"), DSL.field("u.email"))
    .from(u)
    .where(DSL.condition("u.active = true"))
    .fetch()
```

> **권장**: 코드 생성된 `USERS` 클래스가 있다면 `DSL.table()`보다 생성된 클래스를 사용하세요.
> `DSL.table()`은 jOOQ 코드 생성을 아직 적용하지 못한 레거시 테이블에 유용합니다.

---

## DSL.sql() - 완전한 SQL 조각

구조화된 `Field`나 `Condition`이 아닌, 완전한 SQL 조각을 그대로 삽입할 때 사용합니다.

```kotlin
// 특수 문법을 그대로 사용
val queryPart: QueryPart = DSL.sql("AT TIME ZONE 'UTC'")

// 주로 Field 뒤에 붙이는 경우
val utcTime = DSL.field("{0} AT TIME ZONE 'UTC'", LocalDateTime::class.java, EVENTS.CREATED_AT)
```

---

## DSL.inline() - 리터럴 값 (bind variable 대신)

bind variable(`?`) 대신 SQL에 값을 직접 인라인할 때 사용합니다.

```kotlin
// bind variable 방식 (기본, 권장)
dsl.selectFrom(USERS).where(USERS.STATUS.eq("ACTIVE"))
// → WHERE status = ?  (값이 ? 로 바인딩)

// inline 방식 (값이 SQL에 직접 포함)
dsl.selectFrom(USERS).where(USERS.STATUS.eq(DSL.inline("ACTIVE")))
// → WHERE status = 'ACTIVE'
```

**inline() 사용 시나리오:**
- 쿼리 플랜 캐시를 활용하고 싶지 않을 때
- 특정 DB 기능이 리터럴 값만 허용할 때 (예: `LIMIT` 인자)
- Plain SQL 템플릿 내부에서 안전하게 값을 삽입할 때

```kotlin
// Plain SQL 템플릿에서 사용자 입력값을 안전하게 삽입
fun searchProducts(category: String, minPrice: Int): List<Record> {
    // 올바른 방법: bind variable 또는 inline 사용
    val cond = DSL.condition(
        "category = {0} AND price >= {1}",
        DSL.inline(category),   // 안전
        DSL.inline(minPrice)    // 안전
    )

    // 잘못된 방법: 문자열 직접 연결 (SQL Injection 위험!)
    // val cond = DSL.condition("category = '$category'")  // 절대 금지

    return dsl.select().from(PRODUCTS).where(cond).fetch()
}
```

---

## 실전 예제: 레거시 SQL을 점진적으로 전환

```kotlin
// 1단계: 전체를 raw SQL로 실행 (초기 마이그레이션)
fun findTopSellers_v1(): List<Record> {
    return dsl.fetch(
        "SELECT p.id, p.name, SUM(oi.quantity) as total_sold " +
        "FROM products p " +
        "JOIN order_items oi ON p.id = oi.product_id " +
        "GROUP BY p.id, p.name " +
        "ORDER BY total_sold DESC " +
        "LIMIT 10"
    )
}

// 2단계: DSL로 점진적으로 전환 (혼합)
fun findTopSellers_v2(): List<Record> {
    return dsl
        .select(
            PRODUCTS.ID,
            PRODUCTS.NAME,
            DSL.field("SUM({0})", Long::class.java, ORDER_ITEMS.QUANTITY).`as`("total_sold")
        )
        .from(PRODUCTS)
        .join(ORDER_ITEMS).on(PRODUCTS.ID.eq(ORDER_ITEMS.PRODUCT_ID))
        .groupBy(PRODUCTS.ID, PRODUCTS.NAME)
        .orderBy(DSL.field("total_sold", Long::class.java).desc())
        .limit(10)
        .fetch()
}

// 3단계: 완전한 jOOQ DSL (목표)
fun findTopSellers_v3(): List<Record> {
    return dsl
        .select(
            PRODUCTS.ID,
            PRODUCTS.NAME,
            sum(ORDER_ITEMS.QUANTITY).`as`("total_sold")
        )
        .from(PRODUCTS)
        .join(ORDER_ITEMS).on(PRODUCTS.ID.eq(ORDER_ITEMS.PRODUCT_ID))
        .groupBy(PRODUCTS.ID, PRODUCTS.NAME)
        .orderBy(sum(ORDER_ITEMS.QUANTITY).desc())
        .limit(10)
        .fetch()
}
```

---

## Plain SQL 사용 시 체크리스트

사용 전 아래를 확인하세요:

- [ ] jOOQ DSL로 표현할 방법이 정말 없는가?
- [ ] 사용자 입력이 포함된다면 bind variable(`{0}`)이나 `DSL.inline()`을 사용하는가?
- [ ] 문자열을 직접 이어 붙이는 코드가 없는가? (`"WHERE name = '" + input + "'"` 형태 금지)
- [ ] 타입 파라미터(`String::class.java`, `Long::class.java`)가 올바른가?

---

> **참고**: Plain SQL 관련 메서드 요약
>
> | 메서드 | 반환 타입 | 사용 위치 |
> |--------|-----------|-----------|
> | `DSL.field("sql", type)` | `Field<T>` | SELECT, WHERE, ORDER BY |
> | `DSL.condition("sql")` | `Condition` | WHERE |
> | `DSL.table("name")` | `Table<Record>` | FROM, JOIN |
> | `DSL.sql("fragment")` | `QueryPart` | 그 외 SQL 조각 |
> | `DSL.inline(value)` | `Field<T>` | Plain SQL 템플릿 내부 값 |
