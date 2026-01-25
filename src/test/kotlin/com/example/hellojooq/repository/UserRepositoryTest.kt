package com.example.hellojooq.repository

import com.example.hellojooq.domain.User
import com.example.hellojooq.domain.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `findAll should return all users`() {
        // given: 샘플 데이터가 이미 존재 (V2__insert_sample_data.sql)

        // when
        val users = userRepository.findAll()

        // then
        assertThat(users).isNotEmpty
        assertThat(users.size).isGreaterThanOrEqualTo(4)
    }

    @Test
    fun `findByEmail should return user with matching email`() {
        // given
        val email = "john@example.com"

        // when
        val user = userRepository.findByEmail(email)

        // then
        assertThat(user).isNotNull
        assertThat(user?.email).isEqualTo(email)
        assertThat(user?.name).isEqualTo("John Doe")
    }

    @Test
    fun `findByStatus should return users with matching status`() {
        // when
        val activeUsers = userRepository.findByStatus(UserStatus.ACTIVE)

        // then
        assertThat(activeUsers).isNotEmpty
        assertThat(activeUsers).allMatch { it.status == UserStatus.ACTIVE }
    }

    @Test
    fun `save should create new user`() {
        // given
        val newUser = User(
            name = "New User",
            email = "newuser@example.com",
            status = UserStatus.ACTIVE
        )

        // when
        val savedUser = userRepository.save(newUser)

        // then
        assertThat(savedUser.id).isNotNull()
        assertThat(savedUser.name).isEqualTo("New User")
        assertThat(savedUser.email).isEqualTo("newuser@example.com")
        assertThat(savedUser.createdAt).isNotNull()
    }

    @Test
    fun `update should modify existing user`() {
        // given
        val user = userRepository.findByEmail("john@example.com")!!
        val updatedUser = user.copy(name = "John Updated")

        // when
        val result = userRepository.update(updatedUser)

        // then
        assertThat(result.name).isEqualTo("John Updated")
    }

    @Test
    fun `existsByEmail should return true for existing email`() {
        // when
        val exists = userRepository.existsByEmail("john@example.com")

        // then
        assertThat(exists).isTrue()
    }

    @Test
    fun `existsByEmail should return false for non-existing email`() {
        // when
        val exists = userRepository.existsByEmail("nonexistent@example.com")

        // then
        assertThat(exists).isFalse()
    }

    @Test
    fun `count should return total number of users`() {
        // when
        val count = userRepository.count()

        // then
        assertThat(count).isGreaterThanOrEqualTo(4)
    }
}
