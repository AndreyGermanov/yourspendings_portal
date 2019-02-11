package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import ru.itport.yourspendings.entity.PurchaseProduct

interface PurchaseProductsRepository: JpaRepository<PurchaseProduct,Int>