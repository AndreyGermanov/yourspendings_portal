package ru.itport.yourspendings.clouddb

import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component


@Component
class FirestoreService(firebaseApp: FirebaseApplication,env:Environment) {

    lateinit var firestore: Firestore

    init {
        if (!env.activeProfiles.contains("development")) {
            firebaseApp.initialize()
            firestore = FirestoreClient.getFirestore()
        }
    }

    fun collection(name:String): CollectionReference {
        return firestore.collection(name)
    }
}