package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.config.Projection
import ru.itport.yourspendings.entity.*
import java.util.*

interface PurchasesRepository: JpaRepository<Purchase, String>


