package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.PurchaseUser

@RestController
@RequestMapping("/api/purchaseUser")
class PurchaseUsersController:EntityController<PurchaseUser>("PurchaseUser") {
    override fun getItemId(id: Any): Any = id.toString()
}