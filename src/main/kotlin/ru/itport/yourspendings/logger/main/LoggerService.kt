package main

import aggregators.SimpleFileDataAggregator
import archivers.DataArchiver
import archivers.ZipArchiveExtractor
import cleaners.DataCleaner
import config.ConfigManager
import cronjobs.Cronjob
import cronjobs.ICronjobTask
import db.persisters.FileDatabasePersister
import loggers.Logger
import rotators.FileRotator

import java.util.*
import java.util.stream.Collectors

/**
 * Service which manages all modules, related to logging (loggers,aggregators, persisters).
 * Used to create schedule and run configured components as cronjobs
 * Implemented as singleton.
 */
class LoggerService
/**
 * Private constructor
 */
private constructor() {

    /// Determines if service already started
    private var started = false
    /// Array of started service cronjobs indexed by names
    private val cronjobs = HashMap<String, Cronjob>()
    /// Link to configuration manager, which provides configuration objects for cronjobs
    private val configManager = ConfigManager.getInstance()

    /**
     * Returns list of unique names of cronjobs, created from config file
     * @return List of cronjob names
     */
    val cronjobNames: Set<String>
        get() = cronjobs.keys

    /**
     * Returns list of cronjob types
     * @return List of cronjob types
     */
    val cronjobTypes: Set<String>
        get() = cronjobs.values.stream().map { it -> it.task.collectionType }.collect(Collectors.toSet())

    /**
     * Method used to start service
     */
    fun start() {
        if (started) return
        val collections = arrayOf("loggers", "aggregators", "persisters", "archivers", "extractors", "rotators", "cleaners")
        Arrays.stream(collections).forEach( { this.startCronjobs(it) })
        this.started = true
    }

    /**
     * Method used to setup cronjobs for all configured modules in collection of specified type
     * @param collectionType Type of module (loggers,aggregators, persisters etc.)
     */
    private fun startCronjobs(collectionType: String) {
        val collection = configManager.getConfigCollection(collectionType)
        if (collection == null || collection.size == 0) return
        collection.forEach { name, value ->
            val cronjob = createCronjob(collectionType, name)
            if (cronjob != null) {
                collection.forEach {
                    val task = cronjob!!.task
                    cronjobs[task.collectionType + "_" + task.name] = cronjob
                    Timer().scheduleAtFixedRate(cronjob, 0, (cronjob.pollPeriod * 1000).toLong())
                }
            }
        }
    }

    /**
     * Method used to create cronjob instance for specified module for Timer thread
     * @param collectionType - Type of module collection (loggers,aggregators,persisters)
     * @param objectName - System name of object
     * @return Initialized cronjob instance
     */
    private fun createCronjob(collectionType: String, objectName: String): Cronjob? {
        val objectConfig = configManager.getConfigNode(collectionType, objectName)
        val pollPeriod = java.lang.Double.valueOf((objectConfig as java.util.Map<String, Any>).getOrDefault("pollPeriod", 5).toString()).toInt()
        val task = createCronjobTask(collectionType, objectConfig) ?: return null
        return Cronjob(task, pollPeriod)
    }

    /**
     * Method used to load component which used as a task inside cronjob
     * @param collectionType - Type of module collection (loggers,aggregators,persisters)
     * @param objectConfig - Configuration object, used to construct and configure object
     * @return Object which implements ICronjoTask interface and used inside cronjob as a task
     */
    private fun createCronjobTask(collectionType: String, objectConfig: HashMap<String, Any>): ICronjobTask? {
        when (collectionType) {
            "loggers" -> return Logger.create(objectConfig)
            "aggregators" -> return SimpleFileDataAggregator(objectConfig)
            "persisters" -> return FileDatabasePersister(objectConfig)
            "archivers" -> return DataArchiver.create(objectConfig)
            "extractors" -> return ZipArchiveExtractor(objectConfig)
            "rotators" -> return FileRotator(objectConfig)
            "cleaners" -> return DataCleaner(objectConfig)
            else -> return null
        }
    }


    fun getCronjobTask(name: String): ICronjobTask? {
        return if (!cronjobs.containsKey(name)) null else cronjobs[name]!!.task
    }

    companion object {

        /// Link to single instance of this class
        private var instance: LoggerService? = null

        /**
         * Method used to get instance of service from other classes.
         * @return Instance of application
         */
        fun getInstance(): LoggerService {
            if (instance == null) instance = LoggerService()
            return instance!!
        }
    }
}