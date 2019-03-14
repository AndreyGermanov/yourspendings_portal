package ru.itport.yourspendings.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.clouddb.CloudDBService
import ru.itport.yourspendings.entity.*
import java.util.*
import javax.transaction.Transactional

@RestController
@RequestMapping("/api/purchase")
class PurchasesController:EntityController<Purchase>("Purchase") {

    @Autowired
    lateinit var cloudService: CloudDBService

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

    @GetMapping("/sync")
    @Transactional
    fun sync():Any {
        BaseModel.setup(entityManager)
        syncUsers()
        syncShops()
        syncPurchases()
        return hashMapOf("status" to "ok")
    }

    fun syncUsers() { cloudService.getLastData("users",BaseModel.getLastUpdateTimestamp("PurchaseUser"))
            .forEach { createUser(it).also { entityManager.persist(it) } }
    }

    fun syncShops() {
        cloudService.getLastData("shops",BaseModel.getLastUpdateTimestamp("Shop") ).also {
            it.forEach { createShop(it).also { entityManager.persist(it) } }
        }
    }

    fun syncPurchases() =
        cloudService.getLastData("purchases",BaseModel.getLastUpdateTimestamp("Purchase")).also {
            it.forEach { val data=it; createPurchase(it).also {
                entityManager.persist(it)
                syncPurchaseImages(data["images"] as? MutableMap<String,String> ,it)
            } }
        }

    private fun syncPurchaseImages(images:MutableMap<String,String>?,purchase:Purchase) {
        images?.let { it.forEach {
            createPurchaseImage(purchase,it.key,it.value).also { entityManager.persist(it) }
        } }
    }

    fun createUser(data:MutableMap<String,Any>): PurchaseUser = BaseModel.createModel("ru.itport.yourspendings.entity.PurchaseUser",hashMapOf(
            "uid" to data["id"].toString(),
            "name" to data["name"].toString(),
            "email" to data["email"].toString(),
            "phone" to data["phone"].toString(),
            "isDisabled" to data["disabled"].toString().toBoolean(),
            "updatedAt" to data["updated_at"] as Date
        )
    ) as PurchaseUser

    fun createShop(data:MutableMap<String,Any>):Shop =
        BaseModel.createModel("ru.itport.yourspendings.entity.Shop",hashMapOf(
            "uid" to data["id"]!!.toString(),
            "name" to data["name"].toString(),
            "latitude" to (data["latitude"].toString().toDoubleOrNull() ?: 0.0),
            "longitude" to (data["longitude"].toString().toDoubleOrNull() ?: 0.0),
            "user" to data["user_id"].toString(),
            "updatedAt" to Date((data["updated_at"]?.toString()?.toLong() ?: 0)*1000)
        )
    ) as Shop

    fun createPurchase(data:MutableMap<String,Any>): Purchase = BaseModel.createModel("ru.itport.yourspendings.entity.Purchase",
        hashMapOf(
            "uid" to data["id"]!!.toString(),
            "date" to data["date"] as Date,
            "place" to data["place_id"].toString(),
            "user" to  data["user_id"].toString(),
            "updatedAt" to Date((data["updated_at"]?.toString()?.toLong() ?: 0)*1000)
        )
    ) as Purchase

    private fun createPurchaseImage(purchase:Purchase,id:String,timestamp:String): PurchaseImage =
        BaseModel.createModel("ru.itport.yourspendings.entity.PurchaseImage",hashMapOf(
            "uid" to id,
            "timestamp" to timestamp,
            "purchase" to purchase.uid
        ) as HashMap<String,Any>
    ) as PurchaseImage

}