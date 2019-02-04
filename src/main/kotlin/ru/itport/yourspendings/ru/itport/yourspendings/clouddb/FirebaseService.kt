package ru.itport.yourspendings.ru.itport.yourspendings.clouddb

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import ru.itport.yourspendings.ru.itport.yourspendings.entity.Purchase
import ru.itport.yourspendings.ru.itport.yourspendings.entity.PurchaseImage
import ru.itport.yourspendings.ru.itport.yourspendings.entity.Shop
import ru.itport.yourspendings.ru.itport.yourspendings.ru.itport.yourspendings.dao.PurchaseImagesRepository
import ru.itport.yourspendings.ru.itport.yourspendings.ru.itport.yourspendings.dao.PurchasesRepository
import ru.itport.yourspendings.ru.itport.yourspendings.ru.itport.yourspendings.dao.ShopsRepository
import java.io.FileInputStream
import java.util.*

@Suppress("UNCHECKED_CAST")
@Component
@ConfigurationProperties("cloudservice")
class FirebaseService: CloudDBService {

    var syncInterval:Long = 0
    var configFilePath:String=""
    var databaseName:String=""

    lateinit var timer: Timer

    @Autowired lateinit var shopsRepository: ShopsRepository
    @Autowired lateinit var purchasesRepository: PurchasesRepository
    @Autowired lateinit var purchaseImagesRepository: PurchaseImagesRepository

    override fun init() {
        FirebaseApp.initializeApp(FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(FileInputStream(configFilePath)))
                .setDatabaseUrl(databaseName).build())
    }

    override fun startDataSync() {
        timer = Timer().apply { scheduleAtFixedRate(SyncTask(),0,syncInterval) }
    }

    override fun stopDataSync() = timer.cancel()

    override fun syncData(callback: () -> Unit) {
        FirestoreClient.getFirestore().apply { syncShops(this);syncPurchases(this)}
    }

    fun syncShops(db: Firestore) {
        db.collection("shops").get().get().documents.forEach { val data = it.data;
            Shop(
                id = data["id"]!!.toString(),
                name = data["name"].toString(),
                latitude = data["latitude"].toString().toDoubleOrNull() ?: 0.0,
                longitude = data["longitude"].toString().toDoubleOrNull() ?: 0.0,
                userId = data["userId"].toString()
            ).also { shopsRepository.save(it) }
        }
    }

    fun syncPurchases(db: Firestore) {
        db.collection("purchases").get().get().documents.forEach { val data = it.data
            Purchase(
                id = data["id"]!!.toString(),
                date = data["date"] as Date,
                place = shopsRepository.findById(data["place_id"]!!.toString()).orElse(null),
                userId = data["user_id"]!!.toString()
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

    inner class SyncTask: TimerTask() {
        override fun run() {
            this@FirebaseService.syncData {  }
        }

    }
}