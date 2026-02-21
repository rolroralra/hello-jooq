# Hello jOOQ

jOOQ를 학습하고 실습하기 위한 Spring Boot 프로젝트입니다.

## 기술 스택

- **Java 21**
- **Kotlin 1.9.25**
- **Spring Boot 3.4.1**
- **jOOQ 3.19.16**
- **PostgreSQL 16** (Docker Compose)
- **Flyway** (데이터베이스 마이그레이션)
- **Gradle Kotlin DSL**
- **Docker Compose** (테스트용 PostgreSQL)

## 프로젝트 구조

```
hello-jooq/
├── docs/                           # jOOQ 교육 문서
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/example/hellojooq/
│   │   │       ├── config/         # 설정 클래스
│   │   │       ├── controller/     # REST 컨트롤러
│   │   │       ├── domain/         # 도메인 모델
│   │   │       ├── repository/     # jOOQ Repository
│   │   │       └── service/        # 비즈니스 로직
│   │   └── resources/
│   │       ├── db/migration/       # Flyway 마이그레이션
│   │       └── application.yml     # 애플리케이션 설정
│   └── test/
│       ├── kotlin/                 # 테스트 코드
│       └── resources/              # 테스트 설정 (application-test.yml)
├── compose.yaml                    # Docker Compose 설정
└── build/
    └── generated/jooq/main/        # jOOQ 생성 코드
```

## 사전 요구사항

- **Docker Desktop** (또는 Docker Engine + Docker Compose)
- **Java 21**
- **Gradle** (또는 Gradle Wrapper 사용)

## 시작하기

### 1. 프로젝트 클론

```bash
git clone <repository-url>
cd hello-jooq
```

### 2. Docker로 PostgreSQL 실행

Spring Boot Docker Compose Support가 자동으로 컨테이너를 관리합니다.
애플리케이션 실행 시 자동으로 PostgreSQL 컨테이너가 시작됩니다.

수동으로 실행하려면:
```bash
docker compose up -d
```

### 3. jOOQ 코드 생성

PostgreSQL 컨테이너가 실행 중인 상태에서:

```bash
# Flyway 마이그레이션 실행 및 jOOQ 코드 생성
./gradlew generateJooq
```

### 4. 애플리케이션 빌드 및 실행

```bash
# 전체 빌드
./gradlew build

# 애플리케이션 실행 (Docker Compose 자동 시작)
./gradlew bootRun
```

애플리케이션이 `http://localhost:8080`에서 실행됩니다.

### 5. 테스트 실행

테스트는 Docker Compose로 실행된 PostgreSQL을 사용합니다.
**테스트 실행 전 PostgreSQL 컨테이너가 실행 중이어야 합니다.**

```bash
# PostgreSQL 컨테이너 실행 (이미 실행 중이면 생략)
docker compose up -d

# 테스트 실행
./gradlew test
```

## Docker Compose 설정

`compose.yaml` 파일에 PostgreSQL 설정이 정의되어 있습니다:

```yaml
services:
  postgres:
    image: 'postgres:16-alpine'
    environment:
      POSTGRES_DB: hellojooq
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - '5432:5432'
```

Spring Boot Docker Compose Support 덕분에:
- 앱 시작 시 자동으로 컨테이너 시작
- 앱 종료 시 자동으로 컨테이너 중지

## 데이터베이스 접속

### psql로 접속

```bash
docker compose exec postgres psql -U postgres -d hellojooq
```

### 접속 정보

- Host: `localhost`
- Port: `5432`
- Database: `hellojooq`
- Username: `postgres`
- Password: `postgres`

## API 엔드포인트

### Users API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users` | 전체 사용자 조회 |
| GET | `/api/users/{id}` | ID로 사용자 조회 |
| GET | `/api/users/active` | 활성 사용자 조회 |
| GET | `/api/users/count` | 사용자 수 조회 |
| POST | `/api/users` | 사용자 생성 |
| PUT | `/api/users/{id}` | 사용자 수정 |
| DELETE | `/api/users/{id}` | 사용자 삭제 |

### Products API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/products` | 전체 상품 조회 |
| GET | `/api/products/{id}` | ID로 상품 조회 |
| GET | `/api/products/search?keyword=` | 상품 검색 |
| GET | `/api/products/price-range?minPrice=&maxPrice=` | 가격대별 조회 |
| GET | `/api/products/category/{categoryId}` | 카테고리별 조회 |
| GET | `/api/products/categories` | 전체 카테고리 조회 |
| GET | `/api/products/categories/product-counts` | 카테고리별 상품 수 |
| POST | `/api/products` | 상품 생성 |
| PATCH | `/api/products/{id}/stock?quantity=` | 재고 수정 |
| DELETE | `/api/products/{id}` | 상품 삭제 |

### Orders API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/orders/{id}` | ID로 주문 조회 |
| GET | `/api/orders/user/{userId}` | 사용자별 주문 조회 |
| GET | `/api/orders/status/{status}` | 상태별 주문 조회 |
| GET | `/api/orders/with-users` | 주문+사용자 정보 조회 (JOIN) |
| GET | `/api/orders/{id}/items/details` | 주문 상품 상세 (JOIN) |
| GET | `/api/orders/summary/by-user` | 사용자별 주문 통계 |
| POST | `/api/orders` | 주문 생성 |
| PATCH | `/api/orders/{id}/confirm` | 주문 확정 |
| PATCH | `/api/orders/{id}/complete` | 주문 완료 |
| PATCH | `/api/orders/{id}/cancel` | 주문 취소 |

## 샘플 API 요청

### 사용자 생성

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Test User", "email": "test@example.com"}'
```

### 상품 검색

```bash
curl http://localhost:8080/api/products/search?keyword=MacBook
```

### 주문 생성

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      {"productId": 1, "quantity": 1},
      {"productId": 3, "quantity": 2}
    ]
  }'
```

## 문제 해결

### Docker 컨테이너가 시작되지 않을 때

```bash
# 컨테이너 상태 확인
docker compose ps

# 로그 확인
docker compose logs postgres

# 컨테이너 재시작
docker compose down
docker compose up -d
```

### jOOQ 코드 생성 실패 시

1. PostgreSQL 컨테이너가 실행 중인지 확인
2. 5432 포트가 사용 가능한지 확인
3. 데이터베이스 연결 정보 확인

```bash
# PostgreSQL 연결 테스트
docker compose exec postgres psql -U postgres -d hellojooq -c "SELECT 1"
```

## 학습 자료

jOOQ에 대한 자세한 내용은 아래 교육 문서를 참고하세요:

- [Chapter 1: jOOQ 소개](./docs/01-introduction.md)
- [Chapter 2: 프로젝트 설정](./docs/02-project-setup.md)
- [Chapter 3: SELECT 쿼리](./docs/03-select-queries.md)
- [Chapter 4: INSERT, UPDATE, DELETE](./docs/04-data-manipulation.md)
- [Chapter 5: JOIN과 서브쿼리](./docs/05-joins-and-subqueries.md)
- [Chapter 6: Spring Boot 통합](./docs/06-spring-boot-integration.md)
- [Chapter 7: 팁과 모범 사례](./docs/07-tips-and-best-practices.md)
- [부록: jOOQ 핵심 클래스 개념 정리](./docs/appendix-core-classes.md) _(선택)_
- [부록: Window Functions (윈도우 함수)](./docs/appendix-window-functions.md) _(선택)_
- [부록: Plain SQL 템플릿](./docs/appendix-plain-sql.md) _(선택)_
- [부록: ExecuteListener - 쿼리 실행 생명주기 훅](./docs/appendix-execute-listener.md) _(선택)_

## 참고 자료

- [jOOQ 공식 문서](https://www.jooq.org/doc/latest/manual/)
- [Spring Boot + jOOQ 가이드](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.sql.jooq)
- [Spring Boot Docker Compose Support](https://docs.spring.io/spring-boot/reference/features/dev-services.html#features.dev-services.docker-compose)
- [jOOQ GitHub](https://github.com/jOOQ/jOOQ)
