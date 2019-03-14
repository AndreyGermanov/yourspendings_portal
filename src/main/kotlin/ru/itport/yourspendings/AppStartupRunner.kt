package ru.itport.yourspendings

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.logging.Logger

@Component
@Profile("production")
class ProductionStartupRunner: ApplicationRunner {

    val logger: Logger = Logger.getLogger(ProductionStartupRunner::class.java.name)


    @Value("\${cloudservice.syncEnabled}")
    var syncEnabled:Boolean = false

    override fun run(args: ApplicationArguments?) {
        logger.info("App started in production mode")
    }
}

@Component
@Profile("development")
class DevelopmentStartupRunner: ApplicationRunner {

    val logger: Logger = Logger.getLogger(DevelopmentStartupRunner::class.java.name)

    override fun run(args: ApplicationArguments?) {
        logger.info("App started in development mode")

    }
}
