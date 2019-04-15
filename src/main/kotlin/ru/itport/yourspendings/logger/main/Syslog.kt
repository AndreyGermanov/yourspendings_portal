package main

import config.ConfigManager
import rotators.FileRotator
import rotators.IFileRotator
import java.io.*
import java.nio.file.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Class which implements internal error and info logging
 */
class Syslog
/**
 * Class constuctor
 * @param owner Owner of this logger instance
 */
(   /**
     * Owning object
     */
    private val owner: ISyslog.Loggable) : ISyslog {
    // Is log rotation enabled
    private var rotateLogs = true
    // Maximum single log file size to rotate it
    private var maxLogFileSize = 10 * 1024L
    // Maximum number of log archived files in rotation
    private var maxLogFiles = 5
    // Should log archives in rotation be compressed by ZIP
    private var compressArchives = true
    // Log rotation configuration for concrete log levels
    private var logRotatorsConfig = HashMap<String, Any>()

    init {
        this.configure(this.owner.syslogConfig)
    }

    /**
     * Method used to apply configuration to object.
     * @param config: Configuration object
     */
    fun configure(config: HashMap<String, Any>?) {
        if (config == null) return
        rotateLogs = java.lang.Boolean.parseBoolean((config as java.util.Map<String, Any>).getOrDefault("rotateLogs", rotateLogs).toString())
        maxLogFileSize = java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("maxLogFileSize", maxLogFileSize).toString()).toLong()
        maxLogFiles = java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("maxLogFiles", maxLogFiles).toString()).toInt()
        compressArchives = java.lang.Boolean.parseBoolean((config as java.util.Map<String, Any>).getOrDefault("compressArchives", compressArchives).toString())
        try {
            logRotatorsConfig = (config as java.util.Map<String, Any>).getOrDefault("logRotators", logRotatorsConfig) as HashMap<String, Any>
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Method used to log exception, catched by owner
     * @param e: Link to exception
     * @param source: Link to source object, which caught exception
     * @param methodName: Name of method, in which exception caught
     */
    override fun logException(e: Exception, source: Any, methodName: String) {
        var message = "Message: " + e.localizedMessage + "\n Stack trace: \n"
        message += e.stackTrace.map{ it.toString() }.reduce{ s, s1 -> s + "\n" + s1 }
        this.log(ISyslog.LogLevel.ERROR, message, source.javaClass.name, methodName)
    }

    /**
     * Method used to log message, which owner used to write messages
     * @param level: Log level
     * @param message: Text of message
     * @param className: Name of class, which sent request to write message to log
     * @param methodName: Name of method, which sent request to write message to log
     */
    @Synchronized
    override fun log(level: ISyslog.LogLevel, message: String, className: String, methodName: String) {
        val filePath = getLogFilePath(level) ?: return
        try {
            if (!Files.exists(filePath.parent)) Files.createDirectories(filePath.parent)
            if (!Files.exists(filePath.parent)) return
            if (rotateLogs) getFileRotator(level).rotate()
            var openOption = StandardOpenOption.CREATE_NEW
            if (Files.exists(filePath)) {
                openOption = StandardOpenOption.APPEND
            }
            val writer = Files.newBufferedWriter(filePath, openOption)
            val date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            writer.write(date + " - " + owner.name + " - " + message + " (" + className + "," + methodName + ")\n")
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            println("Syslog error: " + e.message)
            e.printStackTrace()
        }

    }

    /**
     * Method used to get full path to log file, based on Log level and on owner object
     * @param level: Log level
     * @return Full path to log file of specified level
     */
    private fun getLogFilePath(level: ISyslog.LogLevel): Path? {
        val logPath = owner.syslogPath ?: return null
        when (level) {
            ISyslog.LogLevel.DEBUG -> return Paths.get(logPath, "debug.log")
            ISyslog.LogLevel.INFO -> return Paths.get(logPath, "info.log")
            ISyslog.LogLevel.WARNING -> return Paths.get(logPath, "warning.log")
            ISyslog.LogLevel.ERROR -> return Paths.get(logPath, "error.log")
            else -> return null
        }
    }

    /**
     * Method used to get FileRotator which will be used to archive log file
     * and rotate log files if needed based on configuration
     * @param level Log level, for which get rotator
     * @return FileRotator class instance, used to archive current log of this "Level"
     */
    private fun getFileRotator(level: ISyslog.LogLevel): IFileRotator {
        val config = ConfigManager.getInstance().getConfigNode("rotators",
                (logRotatorsConfig as java.util.Map<String, Any>).getOrDefault(level.toString(), "").toString())
                ?: return FileRotator(hashMapOf("filePath" to getLogFilePath(level), "maxArchives" to maxLogFiles,
                        "maxSourceFileSize" to maxLogFileSize, "removeSourceFileAfterRotation" to true,
                        "compressArchives" to compressArchives) as HashMap<String,Any>)
        config["filePath"] = getLogFilePath(level)!!
        return FileRotator(config)
    }
}