package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import ru.itport.yourspendings.entity.Discount

interface DiscountsRepository: JpaRepository<Discount, Int>