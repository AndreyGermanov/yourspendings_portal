package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.access.prepost.PreAuthorize
import ru.itport.yourspendings.entity.ProductCategory

@PreAuthorize("hasRole('ROLE_USER')")
interface ProductCategoriesRepository: JpaRepository<ProductCategory,Long> {
    fun findByName(name:String):ProductCategory?
}