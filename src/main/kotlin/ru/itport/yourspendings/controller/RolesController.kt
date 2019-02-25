package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.Role
import javax.swing.text.html.parser.Entity

@RestController
@RequestMapping("/api/roles")
class RolesController:EntityController<Role>("Role")