package ru.itport.yourspendings.clouddb

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.ClassPathResource
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
class FirebaseService: CloudDBService {

    var syncInterval:Long = 0
    var configFilePath:String=""
    var databaseName:String=""

    lateinit var timer: Timer

    var lastTimestamp:HashMap<String,Long?> = HashMap()

    @Autowired lateinit var shopsRepository: ShopsRepository
    @Autowired lateinit var purchasesRepository: PurchasesRepository
    @Autowired lateinit var purchaseImagesRepository: PurchaseImagesRepository
    lateinit var db: Firestore

    override fun init() {
        if (FirebaseApp.getApps().size == 0 )
            FirebaseApp.initializeApp(FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(ClassPathResource(configFilePath).inputStream))
                    .setDatabaseUrl(databaseName).build())
    }

    override fun startDataSync() {
        timer = Timer().apply { scheduleAtFixedRate(SyncTask(),0,syncInterval) }
    }

    override fun stopDataSync() = timer.cancel()

    override fun syncData(callback: () -> Unit) {
        FirestoreClient.getFirestore().apply { db = this
            syncShops();syncPurchases()
        }
    }

    fun syncShops() {
        getLastData("shops",shopsRepository) { val data = it
            Shop(
                id = data["id"]!!.toString(),
                name = data["name"].toString(),
                latitude = data["latitude"].toString().toDoubleOrNull() ?: 0.0,
                longitude = data["longitude"].toString().toDoubleOrNull() ?: 0.0,
                userId = data["user_id"].toString(),
                updatedAt = Date((data["updated_at"]?.toString()?.toLong() ?: 0)*1000)
            ).also { shopsRepository.save(it) }
        }
    }

    fun syncPurchases() {
        getLastData("purchases",purchasesRepository) { val data = it
            Purchase(
                id = data["id"]!!.toString(),
                date = data["date"] as Date,
                place = shopsRepository.findById(data["place_id"]!!.toString()).orElse(null),
                userId = data["user_id"]!!.toString(),
                updatedAt = Date((data["updated_at"]?.toString()?.toLong() ?: 0)*1000)
            ).also {
                purchasesRepository.save(it)
                syncPurchaseImages(data["images"],it)
            }
        }
    }

    fun syncPurchaseImages(images:Any?,purchase:Purchase) {
        (images as? MutableMap<String,String>)?.let {
            it.forEach {
                PurchaseImage(
                    id = it.key,
                    timestamp = it.value.toIntOrNull() ?: 0,
                    purchase = purchase
                ).also { purchaseImagesRepository.save(it)}
            }
        }
    }

    private fun <T: YModel,U>getLastUpdateTimestamp(repository:JpaRepository<T,U>, collection:String):Long {
        var updatedAt = lastTimestamp[collection]
        if (updatedAt == null)
            updatedAt = repository.findAll(Sort.by(Sort.Direction.DESC,"updatedAt"))
                    .firstOrNull()?.updatedAt?.time.apply { lastTimestamp[collection] = this }
        return (updatedAt ?: 0)/1000
    }

    private fun <T:YModel,U> getLastData(collection:String,repository:JpaRepository<T,U>,callback:(MutableMap<String,Any>)->Unit) {
        db.collection(collection).whereGreaterThan("updated_at",getLastUpdateTimestamp(repository,collection))
                .get().get().documents.forEach { callback(it.data) }
    }

    inner class SyncTask: TimerTask() {
        override fun run() {
            this@FirebaseService.syncData {  }
        }

    }
}