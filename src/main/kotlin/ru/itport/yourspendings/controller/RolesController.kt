package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.Role
import javax.swing.text.html.parser.Entity

@RestController
@RequestMapping("/api/role")
class RolesController:EntityController<Role>("Role") {

    override fun getItemId(id:Any):Any {
        return id.toString().toIntOrNull() ?: 0
    }
}