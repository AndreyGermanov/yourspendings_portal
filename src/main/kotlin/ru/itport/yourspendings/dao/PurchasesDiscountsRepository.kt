package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import ru.itport.yourspendings.entity.PurchaseDiscount

interface PurchasesDiscountsRepository: JpaRepository<PurchaseDiscount,Int>