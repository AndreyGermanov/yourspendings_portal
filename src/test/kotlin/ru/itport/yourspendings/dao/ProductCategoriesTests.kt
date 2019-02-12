package ru.itport.yourspendings.dao

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import ru.itport.yourspendings.entity.ProductCategory
import javax.persistence.EntityManager

@RunWith(SpringRunner::class)
@SpringBootTest
class ProductCategoriesTests {

    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @Autowired lateinit var categories: ProductCategoriesRepository
    @Autowired lateinit var entityManager: EntityManager

    @Before
    fun init() {
        jdbcTemplate.execute("delete from product_categories")
    }

    @Test
    fun createCategoryTest() {
        ProductCategory(name="Category 1").also {
            categories.save(it)
            var dbCategory = categories.findByName("Category 2")
            assertNull(dbCategory)
            dbCategory = categories.findByName("Category 1")
            assertNotNull(dbCategory)
        }
    }

    @Test
    @Transactional
    fun subcategoriesTest() {
        ProductCategory(name="Category 1").also {parent ->
            categories.save(parent)
            val child1 = ProductCategory(name="Sub 1",parent=parent).also { categories.save(it)}
            val child2 = ProductCategory(name="Sub 2",parent=parent).also { categories.save(it)}
            entityManager.refresh(parent)
            assertEquals(2,parent.subCategories?.size)
            categories.delete(parent)
            val list = categories.findAll()
            assertEquals(0,list.size)
        }
    }

}