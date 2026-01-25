package com.example.hellojooq.service

import com.example.hellojooq.domain.User
import com.example.hellojooq.domain.UserStatus
import com.example.hellojooq.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository
) {
    fun findById(id: Long): User {
        return userRepository.findById(id)
            ?: throw NoSuchElementException("User not found: $id")
    }

    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    fun findAll(): List<User> {
        return userRepository.findAll()
    }

    fun findActiveUsers(): List<User> {
        return userRepository.findByStatus(UserStatus.ACTIVE)
    }

    @Transactional
    fun create(request: CreateUserRequest): User {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already exists: ${request.email}")
        }

        val user = User(
            name = request.name,
            email = request.email,
            status = UserStatus.ACTIVE
        )
        return userRepository.save(user)
    }

    @Transactional
    fun update(id: Long, request: UpdateUserRequest): User {
        val existingUser = findById(id)

        val updatedUser = existingUser.copy(
            name = request.name ?: existingUser.name,
            email = request.email ?: existingUser.email,
            status = request.status ?: existingUser.status
        )
        return userRepository.update(updatedUser)
    }

    @Transactional
    fun delete(id: Long) {
        if (!userRepository.deleteById(id)) {
            throw NoSuchElementException("User not found: $id")
        }
    }

    fun count(): Long {
        return userRepository.count()
    }
}

data class CreateUserRequest(
    val name: String,
    val email: String
)

data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val status: UserStatus? = null
)
