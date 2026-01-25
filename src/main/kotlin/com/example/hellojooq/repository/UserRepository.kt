package com.example.hellojooq.repository

import com.example.hellojooq.domain.User
import com.example.hellojooq.domain.UserStatus
import com.example.hellojooq.generated.tables.Users.Companion.USERS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class UserRepository(
    private val dsl: DSLContext
) {
    fun findById(id: Long): User? {
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
            .orderBy(USERS.ID)
            .fetchInto(User::class.java)
    }

    fun findByStatus(status: UserStatus): List<User> {
        return dsl.selectFrom(USERS)
            .where(USERS.STATUS.eq(status.name))
            .orderBy(USERS.NAME)
            .fetchInto(User::class.java)
    }

    fun save(user: User): User {
        val now = LocalDateTime.now()
        val record = dsl.insertInto(USERS)
            .set(USERS.NAME, user.name)
            .set(USERS.EMAIL, user.email)
            .set(USERS.STATUS, user.status.name)
            .set(USERS.CREATED_AT, now)
            .set(USERS.UPDATED_AT, now)
            .returning()
            .fetchOne()

        return record?.into(User::class.java)
            ?: throw RuntimeException("Failed to create user")
    }

    fun update(user: User): User {
        val count = dsl.update(USERS)
            .set(USERS.NAME, user.name)
            .set(USERS.EMAIL, user.email)
            .set(USERS.STATUS, user.status.name)
            .set(USERS.UPDATED_AT, LocalDateTime.now())
            .where(USERS.ID.eq(user.id))
            .execute()

        if (count == 0) {
            throw RuntimeException("User not found: ${user.id}")
        }

        return findById(user.id!!)!!
    }

    fun deleteById(id: Long): Boolean {
        return dsl.deleteFrom(USERS)
            .where(USERS.ID.eq(id))
            .execute() > 0
    }

    fun count(): Long {
        return dsl.selectCount()
            .from(USERS)
            .fetchOne(0, Long::class.java) ?: 0L
    }

    fun existsByEmail(email: String): Boolean {
        return dsl.selectCount()
            .from(USERS)
            .where(USERS.EMAIL.eq(email))
            .fetchOne(0, Int::class.java)!! > 0
    }
}
