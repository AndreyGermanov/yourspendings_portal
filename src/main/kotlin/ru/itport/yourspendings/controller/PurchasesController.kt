package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.Purchase

@RestController
@RequestMapping("/api/purchase")
class PurchasesController:EntityController<Purchase>("Purchase") {
    override fun getItem(@PathVariable("id") itemId: String): Any {
        val item:Purchase = super.getItem(itemId) as? Purchase ?: return HashMap<String,String>()
        return hashMapOf(
            "date" to item.date,
            "place" to item.place,
            "uid" to item.uid,
            "user" to item.user,
            "products" to item.products,
            "images" to item.images,
            "purchaseDiscounts" to item.purchaseDiscounts
        )
    }

    override fun getItemId(id: Any): Any = id.toString()

    override fun getFieldPresentationForList(fieldName:String) = when(fieldName) {
        "place" -> "place.name"
        "user" -> "user.email"
        else -> fieldName
    }



}