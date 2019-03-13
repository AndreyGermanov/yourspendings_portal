package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.Purchase

@RestController
@RequestMapping("/api/purchase")
class PurchasesController:EntityController<Purchase>("Purchase") {

    override fun postProcessListItem(item: Purchase): Any {
        return hashMapOf(
                "date" to item.date,
                "place" to item.place,
                "uid" to item.uid,
                "user" to item.user,
                "products" to item.products?.map {
                    hashMapOf(
                            "name" to it.name,
                            "category" to it.category!!.uid,
                            "price" to it.price,
                            "count" to it.count,
                            "discount" to it.discount,
                            "unit" to it.unit!!.uid,
                            "purchase" to it.purchase!!.uid
                    )
                },
                "images" to item.images,
                "purchaseDiscounts" to item.purchaseDiscounts?.map {
                    hashMapOf(
                        "purchase" to it.purchase.uid,
                        "discount" to it.discount.uid,
                        "amount" to it.amount
                    )
                }
        )
    }

    override fun getItemId(id: Any): Any = id.toString()

    override fun getFieldPresentationForList(fieldName:String) = when(fieldName) {
        "place" -> "place.name"
        "user" -> "user.email"
        else -> fieldName
    }



}