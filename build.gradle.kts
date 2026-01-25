import org.jooq.meta.jaxb.Logging

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.flywaydb:flyway-database-postgresql:10.4.1")
        classpath("org.postgresql:postgresql:42.7.2")
    }
}

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("nu.studer.jooq") version "9.0"
    id("org.flywaydb.flyway") version "10.4.1"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Spring Docker Compose Support
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Database - PostgreSQL only
    runtimeOnly("org.postgresql:postgresql")

    // Flyway
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // jOOQ Code Generator
    jooqGenerator("org.postgresql:postgresql")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

// Flyway 설정 (코드 생성용 로컬 PostgreSQL)
flyway {
    url = "jdbc:postgresql://localhost:5432/hellojooq"
    user = "postgres"
    password = "postgres"
    schemas = arrayOf("public")
    locations = arrayOf("filesystem:src/main/resources/db/migration")
    cleanDisabled = false
}

// jOOQ 코드 생성 설정
jooq {
    version.set("3.19.16")

    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = Logging.WARN

                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:5432/hellojooq"
                    user = "postgres"
                    password = "postgres"
                }

                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"

                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        includes = ".*"
                        excludes = "flyway_schema_history"
                    }

                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = false
                        isFluentSetters = true
                        isPojosAsKotlinDataClasses = true
                        isKotlinNotNullPojoAttributes = true
                        isKotlinNotNullRecordAttributes = true
                    }

                    target.apply {
                        packageName = "com.example.hellojooq.generated"
                        directory = "build/generated/jooq/main"
                    }
                }
            }
        }
    }
}

// 생성된 소스를 소스셋에 추가
sourceSets {
    main {
        kotlin {
            srcDir("build/generated/jooq/main")
        }
    }
}

// Flyway 마이그레이션 후 jOOQ 코드 생성
tasks.named("generateJooq") {
    dependsOn("flywayMigrate")

    inputs.files(fileTree("src/main/resources/db/migration"))
        .withPropertyName("migrations")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// 컴파일 전에 jOOQ 코드 생성
tasks.named("compileKotlin") {
    dependsOn("generateJooq")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
