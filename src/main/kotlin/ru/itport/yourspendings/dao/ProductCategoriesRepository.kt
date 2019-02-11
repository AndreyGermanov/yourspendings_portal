package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import ru.itport.yourspendings.entity.ProductCategory

interface ProductCategoriesRepository: JpaRepository<ProductCategory,Long> {
    fun findByName(name:String):ProductCategory?
}