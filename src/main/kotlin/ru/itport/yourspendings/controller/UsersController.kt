package ru.itport.yourspendings.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.User

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('ROLE_ADMIN')")
class UsersController:EntityController<User>("User") {
    override fun getItemId(id:Any):Any = id.toString().toIntOrNull() ?: 0

    override fun postProcessListItem(item: User): User = item.apply { password = ""}

}