package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.PurchaseProduct

@RestController
@RequestMapping("/api/purchaseProduct")
class PurchaseProductsController:EntityController<PurchaseProduct>("PurchaseProduct") {
    override fun getItemId(id: Any): Any = id.toString().toLongOrNull() ?: 0


}