package ru.itport.yourspendings.clouddb

import com.google.cloud.firestore.Query
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Suppress("UNCHECKED_CAST")
@Component
@ConfigurationProperties("cloudservice")
class FirebaseCloudService: CloudDBService {

    @Autowired lateinit var auth: FirebaseAuthService
    @Autowired lateinit var db: FirestoreService

    override fun getLastData(collection:String,timestamp:Long):List<MutableMap<String,Any>> =
        when(collection) {
            "users" ->  auth.list(timestamp)
            else -> db.collection(collection).whereGreaterThan("updated_at", timestamp)
                .orderBy("updated_at", Query.Direction.ASCENDING)
                .get().get().documents.map { it.data; }
    }

}