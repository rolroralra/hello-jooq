# Chapter 2: 프로젝트 설정

## 개요

이 챕터에서는 Spring Boot + jOOQ + Gradle(Kotlin DSL) 환경을 설정하는 방법을 다룹니다.

## 의존성 추가

### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("nu.studer.jooq") version "9.0"  // jOOQ Gradle 플러그인
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    // 데이터베이스 드라이버 (예: PostgreSQL)
    runtimeOnly("org.postgresql:postgresql")

    // jOOQ 코드 생성기용
    jooqGenerator("org.postgresql:postgresql")
}
```

## jOOQ 코드 생성기 설정

jOOQ의 핵심 기능 중 하나는 데이터베이스 스키마로부터 Java/Kotlin 클래스를 자동 생성하는 것입니다.

### build.gradle.kts에 jOOQ 설정 추가

```kotlin
jooq {
    version.set("3.19.16")  // jOOQ 버전

    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN

                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5432/mydb"
                    user = "postgres"
                    password = "password"
                }

                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"  // Kotlin 코드 생성
                    // Java를 원하면: "org.jooq.codegen.JavaGenerator"

                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"

                        // 포함할 테이블 패턴
                        includes = ".*"

                        // 제외할 테이블 패턴
                        excludes = "flyway_schema_history"
                    }

                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = true
                        isFluentSetters = true
                    }

                    target.apply {
                        packageName = "com.example.jooq.generated"
                        directory = "build/generated-src/jooq/main"
                    }
                }
            }
        }
    }
}

// 생성된 소스를 소스셋에 추가
sourceSets {
    main {
        java {
            srcDir("build/generated-src/jooq/main")
        }
    }
}

// 컴파일 전에 jOOQ 코드 생성
tasks.named("compileKotlin") {
    dependsOn("generateJooq")
}
```

## H2 인메모리 DB로 시작하기

개발 초기에는 H2 인메모리 데이터베이스로 시작하면 편리합니다.

### build.gradle.kts

```kotlin
dependencies {
    runtimeOnly("com.h2database:h2")
    jooqGenerator("com.h2database:h2")
}
```

### H2용 jOOQ 설정

```kotlin
jooq {
    configurations {
        create("main") {
            jooqConfiguration.apply {
                jdbc.apply {
                    driver = "org.h2.Driver"
                    url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
                    user = "sa"
                    password = ""
                }

                generator.apply {
                    database.apply {
                        name = "org.jooq.meta.h2.H2Database"
                        inputSchema = "PUBLIC"
                    }
                    // ... 나머지 설정
                }
            }
        }
    }
}
```

## application.yml 설정

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver

  jooq:
    sql-dialect: POSTGRES
```

## 코드 생성 실행

```bash
# Gradle로 jOOQ 코드 생성
./gradlew generateJooq

# 생성된 파일 확인
ls -la build/generated-src/jooq/main/
```

## 생성되는 파일 구조

코드 생성기를 실행하면 다음과 같은 구조의 파일들이 생성됩니다:

```
build/generated-src/jooq/main/
└── com/example/jooq/generated/
    ├── DefaultCatalog.kt       # 카탈로그 정보
    ├── Public.kt               # 스키마 정보
    ├── Keys.kt                 # Primary Key, Foreign Key
    ├── Indexes.kt              # 인덱스 정보
    └── tables/
        ├── Users.kt            # USERS 테이블
        ├── Books.kt            # BOOKS 테이블
        └── records/
            ├── UsersRecord.kt  # USERS 레코드
            └── BooksRecord.kt  # BOOKS 레코드
```

## Spring Boot에서 DSLContext 사용

Spring Boot는 자동으로 `DSLContext` 빈을 생성해줍니다.

```kotlin
@Service
class UserService(
    private val dsl: DSLContext  // 자동 주입
) {
    fun findAll(): List<UsersRecord> {
        return dsl.selectFrom(USERS)
            .fetchInto(UsersRecord::class.java)
    }
}
```

## 문제 해결

### 코드 생성이 안 될 때

1. 데이터베이스 연결 확인
```bash
# PostgreSQL 연결 테스트
psql -h localhost -U postgres -d mydb
```

2. 스키마에 테이블이 있는지 확인
```sql
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public';
```

3. jOOQ 설정의 `includes` 패턴 확인

### 생성된 클래스를 못 찾을 때

`sourceSets` 설정이 제대로 되어 있는지 확인하세요:

```kotlin
sourceSets {
    main {
        java {
            srcDir("build/generated-src/jooq/main")
        }
    }
}
```

## 다음 단계

다음 챕터에서는 생성된 코드를 이용해 SELECT 쿼리를 작성하는 방법을 알아보겠습니다.

[← Chapter 1: jOOQ 소개](./01-introduction.md) | [Chapter 3: SELECT 쿼리 →](./03-select-queries.md)
