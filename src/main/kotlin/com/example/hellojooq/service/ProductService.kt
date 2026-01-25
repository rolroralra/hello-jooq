package com.example.hellojooq.service

import com.example.hellojooq.domain.Category
import com.example.hellojooq.domain.Product
import com.example.hellojooq.repository.CategoryProductCount
import com.example.hellojooq.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository
) {
    fun findById(id: Long): Product {
        return productRepository.findById(id)
            ?: throw NoSuchElementException("Product not found: $id")
    }

    fun findAll(): List<Product> {
        return productRepository.findAll()
    }

    fun findByCategory(categoryId: Long): List<Product> {
        return productRepository.findByCategoryId(categoryId)
    }

    fun searchByName(keyword: String): List<Product> {
        return productRepository.searchByName(keyword)
    }

    fun findByPriceRange(minPrice: BigDecimal, maxPrice: BigDecimal): List<Product> {
        return productRepository.findByPriceRange(minPrice, maxPrice)
    }

    @Transactional
    fun create(request: CreateProductRequest): Product {
        val product = Product(
            name = request.name,
            description = request.description,
            price = request.price,
            stockQuantity = request.stockQuantity,
            categoryId = request.categoryId
        )
        return productRepository.save(product)
    }

    @Transactional
    fun updateStock(productId: Long, quantityChange: Int) {
        if (!productRepository.updateStock(productId, quantityChange)) {
            throw NoSuchElementException("Product not found: $productId")
        }
    }

    @Transactional
    fun delete(id: Long) {
        if (!productRepository.deleteById(id)) {
            throw NoSuchElementException("Product not found: $id")
        }
    }

    // Category 관련
    fun findAllCategories(): List<Category> {
        return productRepository.findAllCategories()
    }

    fun findCategoryById(id: Long): Category {
        return productRepository.findCategoryById(id)
            ?: throw NoSuchElementException("Category not found: $id")
    }

    fun getProductCountByCategory(): List<CategoryProductCount> {
        return productRepository.countProductsByCategory()
    }
}

data class CreateProductRequest(
    val name: String,
    val description: String? = null,
    val price: BigDecimal,
    val stockQuantity: Int = 0,
    val categoryId: Long? = null
)
