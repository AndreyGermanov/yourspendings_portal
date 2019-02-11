package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import ru.itport.yourspendings.entity.DimensionUnit

interface DimensionUnitsRepository: JpaRepository<DimensionUnit,Int>