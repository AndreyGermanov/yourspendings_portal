package main

import config.ConfigManager

import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.HashMap

/**
 * Main application class. Used to load configuration and start required services,
 * which is configured and enabled. This is singleton
 */
class LoggerApplication
/**
 * Class constuctor
 */
private constructor() {

    private var name = "defaultNode"
    private var appPath = ""
    private var cachePath = "cache"
    private var logPath = "logs"
    private var statusPath = "statusPath"
    var syslogConfig = HashMap<String, Any>()
        private set
    private var configManager: ConfigManager? = null

    /**
     * Returns path, which various modules can use to cache their data
     * @return
     */
    fun getCachePath(): String {
        return getAbsolutePath(cachePath)
    }

    fun getLogPath(): String {
        return getAbsolutePath(logPath)
    }

    fun getStatusPath(): String {
        return getAbsolutePath(statusPath)
    }

    fun getAppPath(): String {
        var resultPath = appPath
        if (resultPath.isEmpty()) resultPath = System.getProperty("user.dir")
        if (!Paths.get(resultPath).isAbsolute) resultPath = System.getProperty("user.dir") + "/" + resultPath
        return resultPath
    }

    fun getAbsolutePath(sourceDir: String): String {
        var resultPath = sourceDir
        if (sourceDir.isEmpty()) resultPath = System.getProperty("user.dir")
        if (!Paths.get(resultPath).isAbsolute) resultPath = getAppPath() + "/" + resultPath
        return resultPath
    }

    fun configure(config: HashMap<String, Any>?) {
        name = (config as java.util.Map<String, Any>).getOrDefault("name", name).toString()
        appPath = (config as java.util.Map<String, Any>).getOrDefault("appPath", appPath).toString()
        cachePath = (config as java.util.Map<String, Any>).getOrDefault("cachePath", cachePath).toString()
        logPath = (config as java.util.Map<String, Any>).getOrDefault("logPath", logPath).toString()
        statusPath = (config as java.util.Map<String, Any>).getOrDefault("statusPath", statusPath).toString()
        try {
            syslogConfig = (config as java.util.Map<String, Any>).getOrDefault("syslog", syslogConfig) as HashMap<String, Any>
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun run(args: Array<String>) {
        configManager = ConfigManager.getInstance()
        if (args.size >= 1) configManager!!.setConfigPath(args[0])
        configManager!!.loadConfig()
        this.configure(configManager!!.config)
        setupOutputs()
        //LoggerService.getInstance().start()
        println("Application started ...")
    }

    private fun setupOutputs() {
        val errorLogPath = Paths.get(this.getLogPath() + "/main/error.log")
        val outputLogPath = Paths.get(this.getLogPath() + "/main/output.log")
        try {
            if (Files.notExists(errorLogPath.parent)) Files.createDirectories(errorLogPath.parent)
            if (Files.exists(errorLogPath)) rotateLogFile(errorLogPath)
            if (Files.exists(outputLogPath)) rotateLogFile(outputLogPath)
            System.setErr(PrintStream(FileOutputStream(errorLogPath.toFile())))
            System.setOut(PrintStream(FileOutputStream(outputLogPath.toFile())))
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun rotateLogFile(file: Path) {
        var openOption = StandardOpenOption.CREATE
        val backupFile = Paths.get(file.toString() + ".1")
        if (Files.exists(backupFile))
            openOption = StandardOpenOption.APPEND
        try {
            Files.newBufferedWriter(backupFile, openOption).use { writer -> Files.newBufferedReader(file).use { reader -> writer.write(reader.lines().reduce("") { s, s1 -> s + "\n" + s1 }) } }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    companion object {

        /// Link to single instance of application
        private var application: LoggerApplication? = null

        /**
         * Method used to get instance of application from other classes.
         * @return Instance of application
         */
        val instance: LoggerApplication
            get() {
                if (application == null) application = LoggerApplication()
                return application as LoggerApplication
            }

        /**
         * Entry point of application
         * @param args Command line arguments
         */
        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            LoggerApplication.instance.run(args)
        }
    }
}