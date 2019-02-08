package ru.itport.yourspendings.clouddb

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("firebase")
class FirebaseApplication {

    lateinit var configFilePath:String;
    lateinit var databaseName:String

    fun initialize() {
        if (FirebaseApp.getApps().size == 0 ) {
            FirebaseApp.initializeApp(FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(ClassPathResource(configFilePath).inputStream))
                    .setDatabaseUrl(databaseName).build())
        }
    }
}

