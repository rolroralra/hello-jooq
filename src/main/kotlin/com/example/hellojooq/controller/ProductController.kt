package com.example.hellojooq.controller

import com.example.hellojooq.domain.Category
import com.example.hellojooq.domain.Product
import com.example.hellojooq.repository.CategoryProductCount
import com.example.hellojooq.service.CreateProductRequest
import com.example.hellojooq.service.ProductService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService
) {
    @GetMapping
    fun findAll(): List<Product> {
        return productService.findAll()
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): Product {
        return productService.findById(id)
    }

    @GetMapping("/search")
    fun search(@RequestParam keyword: String): List<Product> {
        return productService.searchByName(keyword)
    }

    @GetMapping("/price-range")
    fun findByPriceRange(
        @RequestParam minPrice: BigDecimal,
        @RequestParam maxPrice: BigDecimal
    ): List<Product> {
        return productService.findByPriceRange(minPrice, maxPrice)
    }

    @GetMapping("/category/{categoryId}")
    fun findByCategory(@PathVariable categoryId: Long): List<Product> {
        return productService.findByCategory(categoryId)
    }

    @PostMapping
    fun create(@RequestBody request: CreateProductRequest): ResponseEntity<Product> {
        val product = productService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(product)
    }

    @PatchMapping("/{id}/stock")
    fun updateStock(
        @PathVariable id: Long,
        @RequestParam quantity: Int
    ): ResponseEntity<Void> {
        productService.updateStock(id, quantity)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        productService.delete(id)
        return ResponseEntity.noContent().build()
    }

    // Category endpoints
    @GetMapping("/categories")
    fun findAllCategories(): List<Category> {
        return productService.findAllCategories()
    }

    @GetMapping("/categories/{id}")
    fun findCategoryById(@PathVariable id: Long): Category {
        return productService.findCategoryById(id)
    }

    @GetMapping("/categories/product-counts")
    fun getProductCountByCategory(): List<CategoryProductCount> {
        return productService.getProductCountByCategory()
    }
}
