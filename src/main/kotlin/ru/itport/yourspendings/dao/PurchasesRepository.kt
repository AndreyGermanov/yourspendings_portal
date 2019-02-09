package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import ru.itport.yourspendings.entity.Purchase

interface PurchasesRepository: JpaRepository<Purchase, String>
