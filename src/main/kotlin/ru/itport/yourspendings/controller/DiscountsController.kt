package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.Discount

@RestController
@RequestMapping("/api/discount")
class DiscountsController:EntityController<Discount>("Discount") {
    override fun getItemId(id: Any): Any = id.toString().toIntOrNull() ?: 0
}