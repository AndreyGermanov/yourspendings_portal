package ru.itport.yourspendings

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import ru.itport.yourspendings.ru.itport.yourspendings.clouddb.CloudDBService
import java.util.logging.Logger

@Component
class AppStartupRunner: ApplicationRunner {

    val logger: Logger = Logger.getLogger(AppStartupRunner::class.java.name)

    @Autowired
    lateinit var cloudService: CloudDBService

    override fun run(args: ApplicationArguments?) {
        cloudService.init()
        cloudService.startDataSync()
    }


}