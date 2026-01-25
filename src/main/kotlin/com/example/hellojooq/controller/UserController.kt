package com.example.hellojooq.controller

import com.example.hellojooq.domain.User
import com.example.hellojooq.service.CreateUserRequest
import com.example.hellojooq.service.UpdateUserRequest
import com.example.hellojooq.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {
    @GetMapping
    fun findAll(): List<User> {
        return userService.findAll()
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): User {
        return userService.findById(id)
    }

    @GetMapping("/active")
    fun findActiveUsers(): List<User> {
        return userService.findActiveUsers()
    }

    @GetMapping("/count")
    fun count(): Map<String, Long> {
        return mapOf("count" to userService.count())
    }

    @PostMapping
    fun create(@RequestBody request: CreateUserRequest): ResponseEntity<User> {
        val user = userService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdateUserRequest
    ): User {
        return userService.update(id, request)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        userService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
