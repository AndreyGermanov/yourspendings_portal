package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.data.rest.core.config.Projection
import org.springframework.security.access.prepost.PreAuthorize
import ru.itport.yourspendings.entity.User

@RepositoryRestResource(excerptProjection = NoPassword::class)

@PreAuthorize("hasRole('ROLE_ADMIN')")
interface UsersRepository: JpaRepository<User, String> {

}

@Projection(name="noPassword",types=[User::class])
interface NoPassword {
    val username:String
    val enabled:Boolean
}