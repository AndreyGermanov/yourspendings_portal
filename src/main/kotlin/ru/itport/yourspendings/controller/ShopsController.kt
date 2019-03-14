package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.Shop

@RestController
@RequestMapping("/api/shop")
class ShopsController:EntityController<Shop>("Shop") {
    override fun getItemId(id: Any): Any = id.toString()

    override fun getFieldPresentationForList(fieldName:String) = when(fieldName) {
        "user" -> "user.email"
        else -> fieldName
    }
}