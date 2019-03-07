package ru.itport.yourspendings.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.User
import javax.persistence.EntityManager

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('ROLE_ADMIN')")
class UsersController:EntityController<User>("User") {
    override fun getItemId(id:Any):Any {
        return id.toString().toIntOrNull() ?: 0
    }
}