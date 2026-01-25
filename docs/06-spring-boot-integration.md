# Chapter 6: Spring Boot 통합

## 개요

이 챕터에서는 Spring Boot와 jOOQ를 함께 사용하는 방법을 배웁니다.

## 자동 설정

Spring Boot는 `spring-boot-starter-jooq`를 통해 jOOQ를 자동으로 설정합니다.

### 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("org.postgresql:postgresql")
}
```

### application.yml 설정

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

### 자동 주입되는 빈들

Spring Boot가 자동으로 생성하는 빈들:

```kotlin
@Configuration
class JooqAutoConfiguration {
    // DataSource로부터 ConnectionProvider 생성
    @Bean
    fun connectionProvider(dataSource: DataSource): DataSourceConnectionProvider

    // TransactionProvider 생성 (Spring 트랜잭션 연동)
    @Bean
    fun transactionProvider(transactionManager: PlatformTransactionManager): SpringTransactionProvider

    // DSLContext 생성
    @Bean
    fun dslContext(configuration: Configuration): DefaultDSLContext
}
```

## DSLContext 사용하기

### 기본 사용법

```kotlin
@Service
class UserService(
    private val dsl: DSLContext  // 자동 주입
) {
    fun findById(id: Int): UsersRecord? {
        return dsl.selectFrom(USERS)
            .where(USERS.ID.eq(id))
            .fetchOne()
    }

    fun findAll(): List<UsersRecord> {
        return dsl.selectFrom(USERS).fetch()
    }
}
```

### Repository 패턴

```kotlin
@Repository
class UserRepository(
    private val dsl: DSLContext
) {
    fun findById(id: Int): User? {
        return dsl.selectFrom(USERS)
            .where(USERS.ID.eq(id))
            .fetchOneInto(User::class.java)
    }

    fun findByEmail(email: String): User? {
        return dsl.selectFrom(USERS)
            .where(USERS.EMAIL.eq(email))
            .fetchOneInto(User::class.java)
    }

    fun findAll(): List<User> {
        return dsl.selectFrom(USERS)
            .fetchInto(User::class.java)
    }

    fun save(user: User): User {
        val record = dsl.newRecord(USERS, user)
        record.store()
        return record.into(User::class.java)
    }

    fun update(user: User): User {
        dsl.update(USERS)
            .set(USERS.NAME, user.name)
            .set(USERS.EMAIL, user.email)
            .where(USERS.ID.eq(user.id))
            .execute()
        return user
    }

    fun deleteById(id: Int): Boolean {
        return dsl.deleteFrom(USERS)
            .where(USERS.ID.eq(id))
            .execute() > 0
    }
}
```

## 트랜잭션 관리

### @Transactional 사용

Spring의 `@Transactional`은 jOOQ와 완벽하게 통합됩니다.

```kotlin
@Service
class OrderService(
    private val dsl: DSLContext
) {
    @Transactional
    fun createOrder(userId: Int, items: List<OrderItem>): Order {
        // 주문 생성
        val order = dsl.insertInto(ORDERS)
            .set(ORDERS.USER_ID, userId)
            .set(ORDERS.STATUS, "PENDING")
            .set(ORDERS.CREATED_AT, LocalDateTime.now())
            .returning()
            .fetchOne()!!

        // 주문 상품 추가
        items.forEach { item ->
            dsl.insertInto(ORDER_ITEMS)
                .set(ORDER_ITEMS.ORDER_ID, order.id)
                .set(ORDER_ITEMS.PRODUCT_ID, item.productId)
                .set(ORDER_ITEMS.QUANTITY, item.quantity)
                .set(ORDER_ITEMS.PRICE, item.price)
                .execute()
        }

        // 총액 계산 및 업데이트
        val total = items.sumOf { it.price * it.quantity.toBigDecimal() }
        dsl.update(ORDERS)
            .set(ORDERS.TOTAL_AMOUNT, total)
            .where(ORDERS.ID.eq(order.id))
            .execute()

        return order.into(Order::class.java)
    }

    @Transactional(readOnly = true)
    fun findOrderById(id: Int): Order? {
        return dsl.selectFrom(ORDERS)
            .where(ORDERS.ID.eq(id))
            .fetchOneInto(Order::class.java)
    }
}
```

### 트랜잭션 전파

```kotlin
@Service
class PaymentService(
    private val dsl: DSLContext,
    private val orderService: OrderService
) {
    @Transactional
    fun processPayment(orderId: Int, amount: BigDecimal) {
        // 결제 처리
        dsl.insertInto(PAYMENTS)
            .set(PAYMENTS.ORDER_ID, orderId)
            .set(PAYMENTS.AMOUNT, amount)
            .set(PAYMENTS.STATUS, "COMPLETED")
            .execute()

        // 주문 상태 업데이트 (같은 트랜잭션)
        orderService.updateOrderStatus(orderId, "PAID")
    }
}
```

## DTO 매핑

### POJO/Data Class 매핑

```kotlin
// Data class 정의
data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime
)

// 매핑 사용
@Service
class UserService(private val dsl: DSLContext) {

    fun findAllUsers(): List<UserDto> {
        return dsl.select(
                USERS.ID,
                USERS.NAME,
                USERS.EMAIL,
                USERS.CREATED_AT
            )
            .from(USERS)
            .fetchInto(UserDto::class.java)
    }
}
```

### RecordMapper 커스텀 매핑

```kotlin
class UserDtoMapper : RecordMapper<Record, UserDto> {
    override fun map(record: Record): UserDto {
        return UserDto(
            id = record.get(USERS.ID)!!,
            name = record.get(USERS.NAME)!!,
            email = record.get(USERS.EMAIL)!!,
            createdAt = record.get(USERS.CREATED_AT)!!
        )
    }
}

// 사용
val users = dsl.selectFrom(USERS)
    .fetch(UserDtoMapper())
```

### JOIN 결과 매핑

```kotlin
data class OrderWithUser(
    val orderId: Int,
    val totalAmount: BigDecimal,
    val userName: String,
    val userEmail: String
)

fun findOrdersWithUsers(): List<OrderWithUser> {
    return dsl.select(
            ORDERS.ID.`as`("orderId"),
            ORDERS.TOTAL_AMOUNT.`as`("totalAmount"),
            USERS.NAME.`as`("userName"),
            USERS.EMAIL.`as`("userEmail")
        )
        .from(ORDERS)
        .join(USERS).on(ORDERS.USER_ID.eq(USERS.ID))
        .fetchInto(OrderWithUser::class.java)
}
```

## 페이징 처리

### Spring Data의 Pageable 연동

```kotlin
@Repository
class UserRepository(private val dsl: DSLContext) {

    fun findAll(pageable: Pageable): Page<UserDto> {
        // 전체 개수 조회
        val total = dsl.selectCount()
            .from(USERS)
            .fetchOne(0, Long::class.java) ?: 0L

        // 정렬 처리
        val orderFields = pageable.sort.map { order ->
            val field = USERS.field(order.property)
            if (order.isAscending) field?.asc() else field?.desc()
        }.filterNotNull()

        // 데이터 조회
        val content = dsl.selectFrom(USERS)
            .orderBy(orderFields)
            .limit(pageable.pageSize)
            .offset(pageable.offset.toInt())
            .fetchInto(UserDto::class.java)

        return PageImpl(content, pageable, total)
    }
}
```

### Slice 사용 (전체 개수 조회 없이)

```kotlin
fun findAllAsSlice(pageable: Pageable): Slice<UserDto> {
    val content = dsl.selectFrom(USERS)
        .orderBy(USERS.ID.asc())
        .limit(pageable.pageSize + 1)  // 다음 페이지 확인용 +1
        .offset(pageable.offset.toInt())
        .fetchInto(UserDto::class.java)

    val hasNext = content.size > pageable.pageSize
    val sliceContent = if (hasNext) content.dropLast(1) else content

    return SliceImpl(sliceContent, pageable, hasNext)
}
```

## 테스트 작성

### 통합 테스트

```kotlin
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    lateinit var dsl: DSLContext

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        // 테스트 데이터 준비
        dsl.deleteFrom(USERS).execute()
        dsl.insertInto(USERS)
            .set(USERS.NAME, "Test User")
            .set(USERS.EMAIL, "test@example.com")
            .execute()
    }

    @Test
    fun `findByEmail should return user`() {
        val user = userRepository.findByEmail("test@example.com")

        assertThat(user).isNotNull
        assertThat(user?.name).isEqualTo("Test User")
    }
}
```

### @DataJdbcTest 사용

```kotlin
@DataJdbcTest
@Import(UserRepository::class)
class UserRepositoryTest {

    @Autowired
    lateinit var dsl: DSLContext

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `save should persist user`() {
        val user = User(name = "New User", email = "new@example.com")

        val saved = userRepository.save(user)

        assertThat(saved.id).isNotNull()
    }
}
```

### Testcontainers 사용

```kotlin
@SpringBootTest
@Testcontainers
class UserRepositoryIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `repository should work with real database`() {
        // 실제 PostgreSQL로 테스트
    }
}
```

## 커스텀 설정

### Configuration 커스터마이징

```kotlin
@Configuration
class JooqConfiguration {

    @Bean
    fun jooqConfiguration(
        connectionProvider: ConnectionProvider,
        transactionProvider: TransactionProvider
    ): DefaultConfiguration {
        return DefaultConfiguration().apply {
            set(connectionProvider)
            set(transactionProvider)
            set(SQLDialect.POSTGRES)

            // SQL 로깅
            set(DefaultExecuteListenerProvider(LoggingExecuteListener()))

            // 성능 설정
            settings().apply {
                isExecuteLogging = true
                queryTimeout = 30  // 30초 타임아웃
            }
        }
    }
}

// SQL 로깅 리스너
class LoggingExecuteListener : DefaultExecuteListener() {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun executeStart(ctx: ExecuteContext) {
        log.debug("Executing: ${ctx.query()}")
    }

    override fun executeEnd(ctx: ExecuteContext) {
        log.debug("Executed in: ${ctx.executeTime()}ms")
    }
}
```

## 다음 단계

다음 챕터에서는 jOOQ 사용 시 알아두면 좋은 팁과 모범 사례를 알아보겠습니다.

[← Chapter 5: JOIN과 서브쿼리](./05-joins-and-subqueries.md) | [Chapter 7: 팁과 모범 사례 →](./07-tips-and-best-practices.md)
