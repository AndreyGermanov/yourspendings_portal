package ru.itport.yourspendings.clouddb

import com.google.firebase.auth.FirebaseAuth
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.util.*

@Component
class FirebaseAuthService(firebaseApp: FirebaseApplication,env: Environment) {

    lateinit var auth:FirebaseAuth

    init {
        if (!env.activeProfiles.contains("development")) {
            firebaseApp.initialize()
            auth = FirebaseAuth.getInstance()
        }
    }

    fun list(timestamp:Long): List<MutableMap<String,Any>> = auth.listUsers(null).values.map {
        hashMapOf(
                "id" to it.uid,
                "email" to it.email,
                "name" to if (it.displayName == null) "" else it.displayName,
                "disabled" to it.isDisabled,
                "phone" to if (it.phoneNumber == null) "" else it.phoneNumber,
                "updated_at" to Date(it.userMetadata.creationTimestamp)
        )
    }.filter { (it["updated_at"] as Date).time > timestamp }

}