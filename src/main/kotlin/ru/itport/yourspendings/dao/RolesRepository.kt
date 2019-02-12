package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.access.prepost.PreAuthorize
import ru.itport.yourspendings.entity.Role

@PreAuthorize("hasRole('ROLE_ADMIN')")
interface RolesRepository: JpaRepository<Role,String>
