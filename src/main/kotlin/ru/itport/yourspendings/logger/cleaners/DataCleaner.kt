package cleaners

import config.ConfigManager
import cronjobs.CronjobTask
import cronjobs.CronjobTaskStatus
import main.ISyslog
import main.LoggerService
import utils.FileUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.ArrayList
import java.util.HashMap
import java.util.Objects

/**
 * Class implements "Data cleaner" tasks, which used to clean older files from specified folder,
 * based on information from "consumers" of this folder. Cleaner should remove only files, which
 * already processed by all consumers, specified in configuration of each cleaner
 */
class DataCleaner : CronjobTask, IDataCleaner, ISyslog.Loggable {

    // Unique name of cleaner
    override var name = ""

    // Source path to clean
    private var sourcePath: Path? = null
    // List of module names, which are consumers of this folder and requires data in it
    private var consumers: ArrayList<String>? = ArrayList()

    /**
     * Method determines the maximum timestamp of data to remove, based on information from consumers
     * of this folder. Cleaner will remove only files, which modification time is less than timestamp,
     * returned by this method
     * @return Timestamp
     */
    private val maxTimestamp: Long
        get() {
            if (consumers == null || consumers!!.size == 0) return 0L
            val service = LoggerService.getInstance()
            return consumers!!.stream()
                    .map { service.getCronjobTask(it) }.filter { Objects.nonNull(it) }
                    .mapToLong({ it!!.lastRecordTimestamp }).min().orElse(0L)
        }

    override val lastRecord: Any?
        get() = null

    override val lastRecordTimestamp: Long
        get() = 0L

    /**
     * Returns a type of collection of tasks, to which current task belongs (loggers, aggregators, archivers etc)
     * @return Collection name as string
     */
    override val collectionType: String
        get() = "cleaners"

    /**
     * Class constructor
     * @param name Name of rotator in configuration cleaners
     */
    constructor(name: String) {
        this.configure(ConfigManager.getInstance().getConfigNode("cleaners", name))
    }

    /**
     * Class constructor
     * @param config Configuration object for cleaner
     */
    constructor(config: HashMap<String, Any>) {
        this.configure(config)
    }

    /**
     * Method used to apply configuration to object.
     * @param config: Configuration object
     */
    override fun configure(config: HashMap<String, Any>?) {
        if (config == null || !config.containsKey("sourcePath")) return
        super.configure(config)
        name = (config as java.util.Map<String, Any>).getOrDefault("name", name).toString()
        sourcePath = Paths.get((config as java.util.Map<String, Any>).getOrDefault("sourcePath", sourcePath).toString())
        try {
            consumers = (config as java.util.Map<String, Any>).getOrDefault("consumers", consumers) as ArrayList<String>
        } catch (e: Exception) {
            syslog!!.logException(e, this, "configure")
        }

    }

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    override fun run() {
        super.run()
        clean()
        taskStatus = CronjobTaskStatus.IDLE
        lastExecTime = Instant.now().epochSecond
    }

    /**
     * Entry method, which starts cleaning process
     */
    override fun clean() {
        if (sourcePath == null || Files.notExists(sourcePath)) return
        val maxTimestamp = maxTimestamp
        try {
            Files.walk(sourcePath)
                    .filter { path ->
                        try {
                            Files.isRegularFile(path) && Files.getLastModifiedTime(path).toInstant().epochSecond < maxTimestamp
                        } catch (e: Exception) {
                            syslog!!.logException(e, this, "clean.filter")
                            false
                        }
                    }
                    .forEach { path ->
                        try {
                            Files.deleteIfExists(path)
                        } catch (e: Exception) {
                            syslog!!.logException(e, this, "clean.delete")
                        }
                    }
        } catch (e: IOException) {
            syslog!!.logException(e, this, "clean")
        }

        FileUtils.removeFolder(sourcePath!!, true)
    }
}
