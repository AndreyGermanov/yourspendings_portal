package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.access.prepost.PreAuthorize
import ru.itport.yourspendings.entity.DimensionUnit

@PreAuthorize("hasRole('ROLE_USER')")
interface DimensionUnitsRepository: JpaRepository<DimensionUnit,Int>