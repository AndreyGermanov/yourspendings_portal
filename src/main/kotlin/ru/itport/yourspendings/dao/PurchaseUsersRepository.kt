package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.access.prepost.PreAuthorize
import ru.itport.yourspendings.entity.PurchaseUser

@PreAuthorize("hasRole('ROLE_USER')")
interface PurchaseUsersRepository: JpaRepository<PurchaseUser,String> {}