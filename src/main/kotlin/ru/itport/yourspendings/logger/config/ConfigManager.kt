package config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.internal.LinkedTreeMap
import main.ISyslog
import main.LoggerApplication
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.nio.file.*
import java.util.Arrays
import java.util.HashMap

/**
 * Class used to manage system configuration
 */
class ConfigManager
/**
 * Class constructor
 */
private constructor() : ISyslog.Loggable {
    // Link to loaded configuration object
    var config: HashMap<String, Any>? = null
        private set
    // Path to main config file
    private var configPath = "config/main.json"

    /**
     * Method defines default empty configuration, if no configuration found on disk
     * @return HashMap with default configuration object
     */
    private val defaultConfig: HashMap<String, Any>
        get() = hashMapOf("loggers" to HashMap<String, Any>(),
                "aggregators" to HashMap<String, Any>(),
                "adapters" to HashMap<String, Any>(),
                "persisters" to HashMap<String, Any>(),
                "archivers" to HashMap<String, Any>(),
                "extractors" to HashMap<String, Any>())

    override val name: String
        get() = "ConfigManager"

    override val syslogPath: String
        get() = LoggerApplication.instance.getLogPath() + "/" + this.name

    override val syslogConfig: HashMap<String, Any>
        get() = LoggerApplication.instance.syslogConfig

    /**
     * Method used to set path to main config file, which overrides default config path "config/main.json"
     * @param configPath Path to config file
     */
    fun setConfigPath(configPath: String) {
        this.configPath = configPath
    }

    /**
     * Method which used to load configuration from files to "config" object
     */
    fun loadConfig() {
        val path = Paths.get(configPath).toAbsolutePath()
        if (!Files.exists(path)) {
            System.err.println("Config file not found: '" + path.toString() + "'")
            System.exit(0)
        } else {
            config = readConfigFile(path)
        }
    }

    fun loadConfig(config: HashMap<String, Any>) {
        this.config = config
    }

    /**
     * Method writes configuration object to file as JSON
     * @param path Path to file
     * @param config Configuration object
     */
    private fun writeConfigToFile(path: Path, config: HashMap<String, Any>?) {
        if (config == null) return
        try {
            if (!Files.exists(path.parent))
                Files.createDirectories(path.parent)
            val gson = GsonBuilder().setPrettyPrinting().create()
            val writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE)
            writer.write(gson.toJson(config))
            writer.close()
        } catch (e: IOException) {
            System.err.println("Could not write config file '" + path.toString() + "'. " +
                    "Error message: '" + e.message + "'")
            System.exit(1)
        }

    }

    /**
     * Method used to read configuration from file
     * @param path Path to configuration file
     * @return Configuration object as HashMap
     */
    private fun readConfigFile(path: Path): HashMap<String, Any>? {
        if (Files.notExists(path)) return null
        try {
            val reader = Files.newBufferedReader(path)
            val configString = reader.lines().reduce { s, s1 -> s + s1 }.orElse("")
            reader.close()
            val gson = Gson()
            val configMap = gson.fromJson(configString, HashMap::class.java) ?: return null
            return parseConfig(path, configMap.clone() as HashMap<String, Any>)
        } catch (e: Exception) {
            System.err.println("Could not parse config from file '" + path.toString() + "'. " +
                    "Error message: " + e.stackTrace.map { it.toString() }.reduce { s,s1 -> s+s1 })
            System.exit(1)
            return null
        }

    }

    /**
     * Method used to recursively parse configuration object to process all "#include" directives,
     * which includes content from additional files
     * @param rootPath: Path of file, from which source config loaded
     * @param config: Source config
     * @return Config object after processing and inclusion of additional files
     */
    private fun parseConfig(rootPath: Path, config: Map<*, *>?): HashMap<String, Any>? {
        if (config == null) return null
        val result = HashMap<String, Any>()
        for (key in config.keys) {
            val configNode = config[key]!!
            if (key.toString() == "#include") {
                var path = Paths.get(configNode.toString())
                if (!path.isAbsolute) path = Paths.get(rootPath.parent.toString(), path.toString())
                val includeConfig = readConfigFile(path)
                if (includeConfig != null) result.putAll(includeConfig)
            } else if (configNode is LinkedTreeMap<*, *>) {
                result[key.toString()] = parseConfig(rootPath, configNode) ?: HashMap<String,Any>()
            } else if (configNode.toString().startsWith("#include")) {
                var path = Paths.get(configNode.toString().replace("#include ", ""))
                if (!path.isAbsolute) path = Paths.get(rootPath.parent.toString(), path.toString())
                result[key.toString()] = readConfigFile(path) ?: HashMap<String,Any>()
            } else {
                result[key.toString()] = configNode
            }
        }
        return result
    }

    // Methods returns configuration for different type of object specified by it's name
    fun getDatabaseAdapter(name: String): HashMap<String, Any>? {
        return getConfigNode("adapters", name)
    }

    fun getDatabasePersister(name: String): HashMap<String, Any>? {
        return getConfigNode("persisters", name)
    }

    fun getDataAggregator(name: String): HashMap<String, Any>? {
        return getConfigNode("aggregators", name)
    }

    fun getDataLogger(name: String): HashMap<String, Any>? {
        return getConfigNode("loggers", name)
    }

    fun getDataArchiver(name: String): HashMap<String, Any>? {
        return getConfigNode("archivers", name)
    }

    /**
     * Method used to return top level collection of configuration objects from root config
     * @param collectionName : Name of collection to return (adapters, persisters, loggers etc.)
     * @return Collection of objects
     */
    fun getConfigCollection(collectionName: String): HashMap<String, HashMap<String, Any>>? {
        return if (config == null || !config!!.containsKey(collectionName) || config!![collectionName] !is HashMap<*, *>) null else config!![collectionName] as HashMap<String, HashMap<String, Any>>
    }

    /**
     * Method used to return configuration object with specified name from specified collection
     * @param collectionName Name of collection (adapters, persisters, loggers etc.)
     * @param nodeName Name of object
     * @return Configuration object as HashMap
     */
    fun getConfigNode(collectionName: String?, nodeName: String?): HashMap<String, Any>? {
        if (collectionName == null || nodeName == null) return null
        val collection = getConfigCollection(collectionName)
        return if (collection == null || !collection.containsKey(nodeName)) null else collection[nodeName]
    }

    companion object {

        // Link to single instance of this object
        private var instance: ConfigManager? = null

        /**
         * Method used to load singleton instance of this class
         * @return instance of Config manager
         */
        fun getInstance(): ConfigManager {
            if (instance == null) instance = ConfigManager()
            return instance as ConfigManager
        }
    }
}