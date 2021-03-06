package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.security.access.prepost.PreAuthorize
import ru.itport.yourspendings.entity.PurchaseProduct

@PreAuthorize("hasRole('ROLE_USER')")
@RepositoryRestResource(exported = false)
interface PurchaseProductsRepository: JpaRepository<PurchaseProduct,Int>
