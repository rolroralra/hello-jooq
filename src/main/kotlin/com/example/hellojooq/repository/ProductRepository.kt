package com.example.hellojooq.repository

import com.example.hellojooq.domain.Category
import com.example.hellojooq.domain.Product
import com.example.hellojooq.generated.tables.Categories.Companion.CATEGORIES
import com.example.hellojooq.generated.tables.Products.Companion.PRODUCTS
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class ProductRepository(
    private val dsl: DSLContext
) {
    fun findById(id: Long): Product? {
        return dsl.selectFrom(PRODUCTS)
            .where(PRODUCTS.ID.eq(id))
            .fetchOneInto(Product::class.java)
    }

    fun findAll(): List<Product> {
        return dsl.selectFrom(PRODUCTS)
            .orderBy(PRODUCTS.ID)
            .fetchInto(Product::class.java)
    }

    fun findByCategoryId(categoryId: Long): List<Product> {
        return dsl.selectFrom(PRODUCTS)
            .where(PRODUCTS.CATEGORY_ID.eq(categoryId))
            .orderBy(PRODUCTS.NAME)
            .fetchInto(Product::class.java)
    }

    fun findByPriceRange(minPrice: BigDecimal, maxPrice: BigDecimal): List<Product> {
        return dsl.selectFrom(PRODUCTS)
            .where(PRODUCTS.PRICE.between(minPrice, maxPrice))
            .orderBy(PRODUCTS.PRICE)
            .fetchInto(Product::class.java)
    }

    fun searchByName(keyword: String): List<Product> {
        return dsl.selectFrom(PRODUCTS)
            .where(PRODUCTS.NAME.likeIgnoreCase("%$keyword%"))
            .orderBy(PRODUCTS.NAME)
            .fetchInto(Product::class.java)
    }

    fun save(product: Product): Product {
        val now = LocalDateTime.now()
        val record = dsl.insertInto(PRODUCTS)
            .set(PRODUCTS.NAME, product.name)
            .set(PRODUCTS.DESCRIPTION, product.description)
            .set(PRODUCTS.PRICE, product.price)
            .set(PRODUCTS.STOCK_QUANTITY, product.stockQuantity)
            .set(PRODUCTS.CATEGORY_ID, product.categoryId)
            .set(PRODUCTS.CREATED_AT, now)
            .set(PRODUCTS.UPDATED_AT, now)
            .returning()
            .fetchOne()

        return record?.into(Product::class.java)
            ?: throw RuntimeException("Failed to create product")
    }

    fun updateStock(productId: Long, quantity: Int): Boolean {
        return dsl.update(PRODUCTS)
            .set(PRODUCTS.STOCK_QUANTITY, PRODUCTS.STOCK_QUANTITY.plus(quantity))
            .set(PRODUCTS.UPDATED_AT, LocalDateTime.now())
            .where(PRODUCTS.ID.eq(productId))
            .execute() > 0
    }

    fun deleteById(id: Long): Boolean {
        return dsl.deleteFrom(PRODUCTS)
            .where(PRODUCTS.ID.eq(id))
            .execute() > 0
    }

    // Category 관련 메서드
    fun findAllCategories(): List<Category> {
        return dsl.selectFrom(CATEGORIES)
            .orderBy(CATEGORIES.NAME)
            .fetchInto(Category::class.java)
    }

    fun findCategoryById(id: Long): Category? {
        return dsl.selectFrom(CATEGORIES)
            .where(CATEGORIES.ID.eq(id))
            .fetchOneInto(Category::class.java)
    }

    // JOIN 예제: 카테고리별 상품 수
    fun countProductsByCategory(): List<CategoryProductCount> {
        return dsl.select(
                CATEGORIES.ID,
                CATEGORIES.NAME,
                DSL.count(PRODUCTS.ID).`as`("productCount")
            )
            .from(CATEGORIES)
            .leftJoin(PRODUCTS).on(CATEGORIES.ID.eq(PRODUCTS.CATEGORY_ID))
            .groupBy(CATEGORIES.ID, CATEGORIES.NAME)
            .orderBy(CATEGORIES.NAME)
            .fetchInto(CategoryProductCount::class.java)
    }
}

data class CategoryProductCount(
    val id: Long,
    val name: String,
    val productCount: Int
)
