package cronjobs

import main.ISyslog
import main.LoggerApplication
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.HashMap

/**
 * Base class for object, which can be started as a Cronjob by LoggerService inside Timer thread
 */
abstract class CronjobTask : ICronjobTask {

    // Is this task enabled (otherwise LoggerService will not run it)
    /**
     * Getters and setters for variables defined above
     */
    override var isEnabled = false
    // Current status of task (RUNNING or IDLE)
    override var taskStatus = CronjobTaskStatus.IDLE
    // Last timestamp when this task started execution
    override var lastStartTime: Long? = 0L
    // Last timestamp when this task finished execution
    override var lastExecTime: Long? = 0L
    // Path, to which task can write status information about progress or result of execution
    open var mstatusPath = ""
    // Link to system logger used to write information about errors or warnings to file
    protected open var syslog: ISyslog? = null
    var syslogConfig = HashMap<String, Any>()
        protected set

    /**
     * Returns various status information about task (descendants should override to provide specific
     * information)
     * @return HashMap with various data
     */
    override val taskInfo: HashMap<String, Any>
        get() = HashMap()

    /**
     * Returns a type of collection of tasks, to which current task belongs (loggers, aggregators, archivers etc)
     * @return Collection name as string
     */
    override val collectionType: String
        get() = ""

    /**
     * Returns serialized information about last record as a string, ready to write to file in "statusPath"
     * @return String representation of last record or null if not able to produce this string
     */
    open val lastRecordString: String?
        get() = null

    /**
     * Method returns path to log files, which Syslog uses to write error, info or warning messages
     * related to work of this data logger
     * @return Full path to directory for log files of this module
     */
    val syslogPath: String
        get() = LoggerApplication.instance.getLogPath() + "/" + collectionType + "/" + this.name

    /**
     * Method used to apply configuration from configuration file
     * @param config Configuration object
     */
    open fun configure(config: HashMap<String, Any>?) {
        if (config == null) return
        isEnabled = java.lang.Boolean.parseBoolean((config as java.util.Map<String, Any>).getOrDefault("enabled", false).toString())
        mstatusPath = (config as java.util.Map<String, Any>).getOrDefault("statusPath", mstatusPath).toString()
        try {
            syslogConfig = (config as java.util.Map<String, Any>).getOrDefault("syslog",
                    LoggerApplication.instance.syslogConfig) as HashMap<String, Any>
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Method which used by Cronjob object to start this task
     */
    override fun run() {
        if (!this.isEnabled) return
        lastStartTime = Instant.now().epochSecond
        taskStatus = CronjobTaskStatus.RUNNING
        if (syslog != null)
            syslog!!.log(ISyslog.LogLevel.DEBUG, "Running task '" + this.name + "'.", this.javaClass.name, "run")
    }

    /**
     * Method returns path to status folder, which persister used to write status files (as timestamp of last
     * written record)
     * @return Full path
     */
    protected fun getStatusPath(): String {
        var resultPath = mstatusPath
        if (resultPath.isEmpty())
            resultPath = LoggerApplication.instance.getStatusPath() + "/" + collectionType + "/" + this.name
        if (!Paths.get(resultPath).isAbsolute)
            resultPath = LoggerApplication.instance.getStatusPath() + "/" + collectionType + "/" + this.name + "/" + resultPath
        return resultPath
    }

    /**
     * Method used to read last written record from file
     * @return Record
     */
    protected fun readLastRecord(): String? {
        val statusPath = Paths.get(this.getStatusPath() + "/last_record")
        if (!Files.exists(statusPath)) return null
        try {
            Files.newBufferedReader(statusPath).use { reader -> return reader.readLine() }
        } catch (e: IOException) {
            syslog!!.log(ISyslog.LogLevel.ERROR, "Could not read last record from '" + statusPath.toString() + "' file",
                    this.javaClass.name, "readLastRecord")
        } catch (e: Exception) {
            syslog!!.log(ISyslog.LogLevel.ERROR, "Could not parse last record value from '" + statusPath.toString() + "' file.",
                    this.javaClass.name, "readLastRecord")
        }

        return null
    }

    /**
     * Method used to write last written record to file as JSON object
     */
    protected fun writeLastRecord() {
        val lastRecordString = lastRecordString ?: return
        val statusPath = Paths.get(this.getStatusPath() + "/last_record")
        try {
            if (!Files.exists(statusPath.parent)) Files.createDirectories(statusPath.parent)
            Files.deleteIfExists(statusPath)
            val writer = Files.newBufferedWriter(statusPath, StandardOpenOption.CREATE_NEW)
            writer.write(lastRecordString)
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            syslog!!.log(ISyslog.LogLevel.ERROR, "Could not write last record '" + lastRecordString +
                    "' to file '" + statusPath.toString() + "'", this.javaClass.name, "writeLastRecord")
        }

    }

}
