# Chapter 4: INSERT, UPDATE, DELETE

## 개요

이 챕터에서는 jOOQ를 사용해 데이터를 조작하는 방법을 배웁니다.

## INSERT

### 기본 INSERT

```kotlin
// SQL: INSERT INTO users (name, email, status) VALUES ('John', 'john@example.com', 'ACTIVE')
dsl.insertInto(USERS)
    .set(USERS.NAME, "John")
    .set(USERS.EMAIL, "john@example.com")
    .set(USERS.STATUS, "ACTIVE")
    .execute()
```

### columns/values 방식

```kotlin
// SQL: INSERT INTO users (name, email, status) VALUES ('John', 'john@example.com', 'ACTIVE')
dsl.insertInto(USERS, USERS.NAME, USERS.EMAIL, USERS.STATUS)
    .values("John", "john@example.com", "ACTIVE")
    .execute()
```

### 여러 행 한 번에 삽입

```kotlin
// SQL: INSERT INTO users (name, email) VALUES ('John', 'john@example.com'), ('Jane', 'jane@example.com')
dsl.insertInto(USERS, USERS.NAME, USERS.EMAIL)
    .values("John", "john@example.com")
    .values("Jane", "jane@example.com")
    .execute()
```

### Record 객체 사용

```kotlin
// 새 레코드 생성
val user = dsl.newRecord(USERS).apply {
    name = "John"
    email = "john@example.com"
    status = "ACTIVE"
}

// 저장
user.store()  // INSERT 실행

// ID가 자동 생성되었다면 바로 접근 가능
println("생성된 ID: ${user.id}")
```

### INSERT RETURNING (생성된 값 반환)

```kotlin
// SQL: INSERT INTO users (name, email) VALUES ('John', 'john@example.com') RETURNING id
val generatedId = dsl.insertInto(USERS)
    .set(USERS.NAME, "John")
    .set(USERS.EMAIL, "john@example.com")
    .returningResult(USERS.ID)
    .fetchOne()
    ?.get(USERS.ID)

// 전체 레코드 반환
val insertedUser = dsl.insertInto(USERS)
    .set(USERS.NAME, "John")
    .set(USERS.EMAIL, "john@example.com")
    .returning()
    .fetchOne()
```

### INSERT ON CONFLICT (UPSERT)

```kotlin
// PostgreSQL: ON CONFLICT DO UPDATE
dsl.insertInto(USERS)
    .set(USERS.EMAIL, "john@example.com")
    .set(USERS.NAME, "John")
    .onConflict(USERS.EMAIL)
    .doUpdate()
    .set(USERS.NAME, "John Updated")
    .execute()

// ON CONFLICT DO NOTHING
dsl.insertInto(USERS)
    .set(USERS.EMAIL, "john@example.com")
    .set(USERS.NAME, "John")
    .onConflict(USERS.EMAIL)
    .doNothing()
    .execute()
```

### SELECT로부터 INSERT

```kotlin
// SQL: INSERT INTO user_archive (id, name, email)
//      SELECT id, name, email FROM users WHERE status = 'DELETED'
dsl.insertInto(USER_ARCHIVE, USER_ARCHIVE.ID, USER_ARCHIVE.NAME, USER_ARCHIVE.EMAIL)
    .select(
        dsl.select(USERS.ID, USERS.NAME, USERS.EMAIL)
            .from(USERS)
            .where(USERS.STATUS.eq("DELETED"))
    )
    .execute()
```

## UPDATE

### 기본 UPDATE

```kotlin
// SQL: UPDATE users SET status = 'INACTIVE' WHERE id = 1
dsl.update(USERS)
    .set(USERS.STATUS, "INACTIVE")
    .where(USERS.ID.eq(1))
    .execute()
```

### 여러 컬럼 업데이트

```kotlin
// SQL: UPDATE users SET name = 'John Doe', email = 'johndoe@example.com' WHERE id = 1
dsl.update(USERS)
    .set(USERS.NAME, "John Doe")
    .set(USERS.EMAIL, "johndoe@example.com")
    .where(USERS.ID.eq(1))
    .execute()
```

### Record 객체로 업데이트

```kotlin
// 기존 레코드 조회
val user = dsl.selectFrom(USERS)
    .where(USERS.ID.eq(1))
    .fetchOne()

// 값 변경
user?.apply {
    name = "Updated Name"
    email = "updated@example.com"
}

// 저장 (UPDATE 실행)
user?.store()
```

### 조건부 업데이트

```kotlin
// SQL: UPDATE users SET last_login = NOW() WHERE id = 1 AND status = 'ACTIVE'
val updatedCount = dsl.update(USERS)
    .set(USERS.LAST_LOGIN, LocalDateTime.now())
    .where(USERS.ID.eq(1))
    .and(USERS.STATUS.eq("ACTIVE"))
    .execute()

if (updatedCount == 0) {
    println("업데이트된 행이 없습니다")
}
```

### 계산된 값으로 업데이트

```kotlin
// SQL: UPDATE users SET login_count = login_count + 1 WHERE id = 1
dsl.update(USERS)
    .set(USERS.LOGIN_COUNT, USERS.LOGIN_COUNT.plus(1))
    .where(USERS.ID.eq(1))
    .execute()

// SQL: UPDATE products SET price = price * 1.1
dsl.update(PRODUCTS)
    .set(PRODUCTS.PRICE, PRODUCTS.PRICE.mul(1.1))
    .execute()
```

### UPDATE RETURNING

```kotlin
// SQL: UPDATE users SET status = 'INACTIVE' WHERE id = 1 RETURNING *
val updatedUser = dsl.update(USERS)
    .set(USERS.STATUS, "INACTIVE")
    .where(USERS.ID.eq(1))
    .returning()
    .fetchOne()
```

### 대량 업데이트

```kotlin
// SQL: UPDATE users SET status = 'INACTIVE' WHERE last_login < '2024-01-01'
val count = dsl.update(USERS)
    .set(USERS.STATUS, "INACTIVE")
    .where(USERS.LAST_LOGIN.lt(LocalDate.of(2024, 1, 1).atStartOfDay()))
    .execute()

println("$count 명의 사용자가 비활성화되었습니다")
```

## DELETE

### 기본 DELETE

```kotlin
// SQL: DELETE FROM users WHERE id = 1
dsl.deleteFrom(USERS)
    .where(USERS.ID.eq(1))
    .execute()

// 또는 delete() 사용
dsl.delete(USERS)
    .where(USERS.ID.eq(1))
    .execute()
```

### 조건부 DELETE

```kotlin
// SQL: DELETE FROM users WHERE status = 'DELETED' AND deleted_at < '2024-01-01'
dsl.deleteFrom(USERS)
    .where(USERS.STATUS.eq("DELETED"))
    .and(USERS.DELETED_AT.lt(LocalDate.of(2024, 1, 1).atStartOfDay()))
    .execute()
```

### Record 객체로 삭제

```kotlin
val user = dsl.selectFrom(USERS)
    .where(USERS.ID.eq(1))
    .fetchOne()

user?.delete()  // DELETE 실행
```

### DELETE RETURNING

```kotlin
// SQL: DELETE FROM users WHERE id = 1 RETURNING *
val deletedUser = dsl.deleteFrom(USERS)
    .where(USERS.ID.eq(1))
    .returning()
    .fetchOne()
```

### 전체 삭제 (주의!)

```kotlin
// SQL: DELETE FROM users (위험!)
// jOOQ는 WHERE 없는 DELETE를 기본적으로 허용하지만, 주의해야 합니다
dsl.deleteFrom(USERS).execute()

// TRUNCATE (더 빠른 전체 삭제)
dsl.truncate(USERS).execute()
```

## 배치 처리

### Batch Insert

```kotlin
val users = listOf(
    UserData("John", "john@example.com"),
    UserData("Jane", "jane@example.com"),
    UserData("Bob", "bob@example.com")
)

// 배치 쿼리 생성
val batch = dsl.batch(
    dsl.insertInto(USERS, USERS.NAME, USERS.EMAIL)
        .values(null as String?, null as String?)
)

// 바인딩 값 추가
users.forEach { user ->
    batch.bind(user.name, user.email)
}

// 실행
val results = batch.execute()
println("삽입된 행 수: ${results.sum()}")
```

### Batch Update/Delete

```kotlin
// 여러 UPDATE를 배치로 실행
val batch = dsl.batch(
    dsl.update(USERS).set(USERS.STATUS, "INACTIVE").where(USERS.ID.eq(1)),
    dsl.update(USERS).set(USERS.STATUS, "INACTIVE").where(USERS.ID.eq(2)),
    dsl.update(USERS).set(USERS.STATUS, "ACTIVE").where(USERS.ID.eq(3))
)
batch.execute()
```

### batchInsert 메서드 사용

```kotlin
// Record 리스트로 배치 삽입
val records = listOf(
    dsl.newRecord(USERS).apply { name = "John"; email = "john@example.com" },
    dsl.newRecord(USERS).apply { name = "Jane"; email = "jane@example.com" },
    dsl.newRecord(USERS).apply { name = "Bob"; email = "bob@example.com" }
)

dsl.batchInsert(records).execute()
```

## 실행 결과 확인

```kotlin
// execute()는 영향받은 행 수를 반환
val affectedRows = dsl.update(USERS)
    .set(USERS.STATUS, "ACTIVE")
    .where(USERS.STATUS.eq("PENDING"))
    .execute()

println("$affectedRows 행이 업데이트되었습니다")

// 0이면 조건에 맞는 행이 없음
if (affectedRows == 0) {
    throw NotFoundException("업데이트할 데이터가 없습니다")
}
```

## 트랜잭션 내에서 처리

```kotlin
// Spring의 @Transactional 사용
@Transactional
fun transferMoney(fromId: Int, toId: Int, amount: BigDecimal) {
    // 출금
    dsl.update(ACCOUNTS)
        .set(ACCOUNTS.BALANCE, ACCOUNTS.BALANCE.minus(amount))
        .where(ACCOUNTS.ID.eq(fromId))
        .execute()

    // 입금
    dsl.update(ACCOUNTS)
        .set(ACCOUNTS.BALANCE, ACCOUNTS.BALANCE.plus(amount))
        .where(ACCOUNTS.ID.eq(toId))
        .execute()
}

// jOOQ 트랜잭션 API 직접 사용
dsl.transaction { config ->
    val tx = DSL.using(config)

    tx.update(ACCOUNTS)
        .set(ACCOUNTS.BALANCE, ACCOUNTS.BALANCE.minus(amount))
        .where(ACCOUNTS.ID.eq(fromId))
        .execute()

    tx.update(ACCOUNTS)
        .set(ACCOUNTS.BALANCE, ACCOUNTS.BALANCE.plus(amount))
        .where(ACCOUNTS.ID.eq(toId))
        .execute()
}
```

## 다음 단계

다음 챕터에서는 JOIN, 서브쿼리 등 고급 쿼리 기능을 알아보겠습니다.

[← Chapter 3: SELECT 쿼리](./03-select-queries.md) | [Chapter 5: JOIN과 서브쿼리 →](./05-joins-and-subqueries.md)
