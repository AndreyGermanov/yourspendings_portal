package ru.itport.yourspendings.dao

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.rest.core.annotation.RestResource
import org.springframework.data.rest.core.config.Projection
import org.springframework.security.access.prepost.PreAuthorize
import ru.itport.yourspendings.entity.User

@PreAuthorize("hasRole('ROLE_ADMIN')")
interface UsersRepository: JpaRepository<User, Int> {
    fun findByName(name:String): User?

    @Query("SELECT u FROM User u WHERE name=?1")
    fun findAllByName(name:String,pageable: Pageable): List<User>?

    @Query("SELECT count(u) FROM User u")
    fun countAll(): Long

}


@Projection(name="noPassword",types=[User::class])
interface NoPassword {
    val name:String
    val enabled:Boolean
}

@RestResource(exported = false)
interface AdminUsersRepository: JpaRepository<User,Int> {
    fun findByName(name:String): User?
}
