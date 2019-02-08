package ru.itport.yourspendings.clouddb

import com.google.cloud.firestore.Query
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import ru.itport.yourspendings.entity.Purchase
import ru.itport.yourspendings.entity.PurchaseImage
import ru.itport.yourspendings.entity.Shop
import ru.itport.yourspendings.dao.PurchaseImagesRepository
import ru.itport.yourspendings.dao.PurchasesRepository
import ru.itport.yourspendings.dao.ShopsRepository
import ru.itport.yourspendings.entity.YModel
import java.util.*

@Suppress("UNCHECKED_CAST")
@Component
@ConfigurationProperties("cloudservice")
open class FirebaseCloudService: CloudDBService {

    var syncInterval:Long = 0
    lateinit var timer: Timer
    var lastTimestamp:HashMap<String,Long?> = HashMap()
    @Autowired lateinit var shopsRepository: ShopsRepository
    @Autowired lateinit var purchasesRepository: PurchasesRepository
    @Autowired lateinit var purchaseImagesRepository: PurchaseImagesRepository
    @Autowired lateinit var db: FirestoreService

    override fun startDataSync() {
        timer = Timer().also { it.scheduleAtFixedRate(object : TimerTask() {
            override fun run() = this@FirebaseCloudService.syncData() }, 0, syncInterval) }
    }

    override fun stopDataSync() = timer.cancel()

    override fun syncData() { syncShops();syncPurchases() }

    fun syncShops() {
        getLastData("shops", shopsRepository).also {
            it.forEach { createShop(it).also { println(shopsRepository); shopsRepository.save(it) } }
        }
    }

    fun syncPurchases() =
        getLastData("purchases",purchasesRepository).also {
            it.forEach { val data=it; createPurchase(it).also {
                purchasesRepository.save(it)
                syncPurchaseImages(data["images"] as? MutableMap<String,String> ,it)
            } }
        }

    fun createShop(data:MutableMap<String,Any>) =
        Shop(
            id = data["id"]!!.toString(),
            name = data["name"].toString(),
            latitude = data["latitude"].toString().toDoubleOrNull() ?: 0.0,
            longitude = data["longitude"].toString().toDoubleOrNull() ?: 0.0,
            userId = data["user_id"].toString(),
            updatedAt = Date((data["updated_at"]?.toString()?.toLong() ?: 0)*1000)
        )

    fun createPurchase(data:MutableMap<String,Any>): Purchase =
        Purchase(
            id = data["id"]!!.toString(),
            date = data["date"] as Date,
            place = shopsRepository.findById(data["place_id"]!!.toString()).orElse(null),
            userId = data["user_id"]!!.toString(),
            updatedAt = Date((data["updated_at"]?.toString()?.toLong() ?: 0)*1000)
        )

    private fun syncPurchaseImages(images:MutableMap<String,String>?,purchase:Purchase) {
        images?.let { it.forEach {
            createPurchaseImage(purchase,it.key,it.value).also { purchaseImagesRepository.save(it) }
        } }
    }

    private fun createPurchaseImage(purchase:Purchase,id:String,timestamp:String): PurchaseImage =
        PurchaseImage(id = id, timestamp = timestamp.toIntOrNull() ?: 0, purchase = purchase)

    open fun <T:YModel,U> getLastData(collection:String,repository:JpaRepository<T,U>):List<MutableMap<String,Any>> {
        return db.collection(collection).whereGreaterThan("updated_at", getLastUpdateTimestamp(repository, collection))
                .orderBy("updated_at", Query.Direction.ASCENDING)
                .get().get().documents.map { it.data; }
    }

    private fun <T: YModel,U>getLastUpdateTimestamp(repository:JpaRepository<T,U>, collection:String):Long =
        lastTimestamp[collection] ?: (repository.findAll(Sort.by(Sort.Direction.DESC,"updatedAt"))
                    .firstOrNull()?.updatedAt?.time.apply { lastTimestamp[collection] = this } ?: 0)/1000
}