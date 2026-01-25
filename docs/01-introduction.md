# Chapter 1: jOOQ 소개

## jOOQ란?

**jOOQ (Java Object Oriented Querying)**는 Java에서 타입 안전한 SQL을 작성할 수 있게 해주는 라이브러리입니다.

데이터베이스 스키마를 기반으로 Java 클래스를 자동 생성하고, 이를 통해 컴파일 타임에 SQL 문법 오류를 잡아낼 수 있습니다.

## 왜 jOOQ를 사용해야 할까요?

### 1. 타입 안전성

```java
// 기존 JDBC 방식 - 런타임에서야 오류 발견
String sql = "SELECT * FROM usres WHERE id = ?";  // 오타가 있어도 컴파일 통과

// jOOQ 방식 - 컴파일 타임에 오류 발견
dsl.select()
   .from(USERS)  // USRES라고 쓰면 컴파일 에러!
   .where(USERS.ID.eq(1))
   .fetch();
```

### 2. IDE 자동 완성 지원

jOOQ는 데이터베이스 스키마를 Java 클래스로 생성하기 때문에, IDE에서 테이블명과 컬럼명을 자동 완성할 수 있습니다.

### 3. SQL을 그대로 사용

ORM(Object-Relational Mapping)과 달리, jOOQ는 SQL 자체를 존중합니다. 복잡한 쿼리도 SQL 문법 그대로 작성할 수 있습니다.

```java
// 복잡한 쿼리도 SQL 문법 그대로
dsl.select(
        AUTHOR.NAME,
        count(BOOK.ID).as("book_count")
    )
    .from(AUTHOR)
    .leftJoin(BOOK).on(BOOK.AUTHOR_ID.eq(AUTHOR.ID))
    .groupBy(AUTHOR.NAME)
    .having(count(BOOK.ID).gt(5))
    .orderBy(count(BOOK.ID).desc())
    .fetch();
```

### 4. 다양한 데이터베이스 지원

jOOQ는 30개 이상의 RDBMS를 지원합니다:
- PostgreSQL
- MySQL / MariaDB
- Oracle
- SQL Server
- H2
- SQLite
- 그 외 다수...

## jOOQ vs 다른 기술들

| 특징 | JDBC | JPA/Hibernate | MyBatis | jOOQ |
|------|------|---------------|---------|------|
| 타입 안전성 | X | 부분적 | X | O |
| SQL 제어 | O | X | O | O |
| 자동 완성 | X | 부분적 | X | O |
| 학습 곡선 | 낮음 | 높음 | 중간 | 중간 |
| 복잡한 쿼리 | 어려움 | 어려움 | 쉬움 | 쉬움 |

## jOOQ의 핵심 개념

### DSLContext

`DSLContext`는 jOOQ의 진입점입니다. 모든 쿼리 작성은 이 객체를 통해 시작됩니다.

```java
// DSLContext 생성
DSLContext dsl = DSL.using(connection, SQLDialect.POSTGRES);

// 쿼리 실행
dsl.select().from(USERS).fetch();
```

### Generated Classes (생성된 클래스들)

jOOQ 코드 생성기를 실행하면 다음과 같은 클래스들이 생성됩니다:

- **테이블 클래스**: 각 테이블에 대응하는 클래스 (`Users`, `Books` 등)
- **레코드 클래스**: 테이블의 한 행을 나타내는 클래스 (`UsersRecord`, `BooksRecord` 등)
- **필드 클래스**: 각 컬럼에 대응하는 필드

```
generated/
├── tables/
│   ├── Users.java       // USERS 테이블
│   ├── Books.java       // BOOKS 테이블
│   └── records/
│       ├── UsersRecord.java
│       └── BooksRecord.java
└── Keys.java            // Primary Key, Foreign Key 정보
```

## 언제 jOOQ를 선택해야 할까요?

jOOQ가 적합한 경우:
- SQL을 잘 알고 있고, SQL 제어권을 유지하고 싶을 때
- 복잡한 쿼리가 많은 프로젝트
- 타입 안전성이 중요한 프로젝트
- 기존 데이터베이스 스키마가 있는 프로젝트

jOOQ가 덜 적합한 경우:
- 매우 단순한 CRUD만 있는 프로젝트
- 객체 중심의 도메인 모델이 필요한 경우 (DDD)
- 데이터베이스 독립성이 매우 중요한 경우

## 다음 단계

다음 챕터에서는 실제로 jOOQ 프로젝트를 설정하는 방법을 알아보겠습니다.

[Chapter 2: 프로젝트 설정 →](./02-project-setup.md)
