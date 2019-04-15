package db.adapters

import config.ConfigManager
import main.ISyslog
import main.LoggerApplication
import main.Syslog
import java.util.ArrayList
import java.util.HashMap

/**
 * Base class for database adapters
 */
abstract class DatabaseAdapter : IDatabaseAdapter, ISyslog.Loggable {


    private var mname = "";

    // Unique name of database adapter
    override var name:String
        get() { return "Database_adapter-" + this.mname}
        set(value) {this.mname = value}

    // Set of collections (tables) with which this data adapter allows to work
    protected var collections = HashMap<String, Any>()
    // Link to System logger to write error and warning messages
    protected lateinit var syslog: ISyslog

    override var syslogConfig: HashMap<String, Any> = HashMap()

    /**
     * Returns path which System logger uses to write messages, related to this adapter
     * @return
     */
    override val syslogPath: String
        get() = LoggerApplication.instance.getLogPath() + "/db/" + this.name + "/"

    val collectionType: String
        get() = "adapters"

    /**
     * Method used to apply configuration to data adapter
     * @param config Configuration object
     */
    override fun configure(config: HashMap<String, Any>) {
        this.name = (config as java.util.Map<String, Any>).getOrDefault("name", "").toString()
        this.collections = (config as java.util.Map<String, Any>).getOrDefault("collections", HashMap<Any, Any>()) as HashMap<String, Any>
        try {
            this.syslogConfig = (config as java.util.Map<String, Any>).getOrDefault("syslog",
                    LoggerApplication.instance.syslogConfig) as HashMap<String, Any>
        } catch (e: Exception) {
            e.printStackTrace()
        }

        this.syslog = Syslog(this)
    }

    /**
     * Public method used by consumers to select data from data source
     * @param sql SQL query text
     * @param collectionName Collection name to which SQL applied, or null, if more than one collection
     * specified in SQL query
     * @return Result as array of rows
     */
    override fun select(sql: String, collectionName: String?): ArrayList<HashMap<String, Any>> {
        return processQueryResult(executeSelectQuery(sql), collectionName!!)!!
    }

    /**
     * Method used to insert set of records to specified collection in database
     * @param collectionName Name of collection
     * @param data List of records
     * @return Number of inserted records
     */
    override fun insert(collectionName: String, data: ArrayList<HashMap<String, Any>>): Int? {
        return processUpdateQuery(collectionName, data, true)
    }

    /**
     * Method used to update specified set of records in specified collection of database
     * @param collectionName Name of collection
     * @param data List of records
     * @return Number of updated records
     */
    override fun update(collectionName: String, data: ArrayList<HashMap<String, Any>>): Int? {
        return processUpdateQuery(collectionName, data, false)
    }

    /**
     * Databases specific method to send SELECT query to server and return RAW result
     * @param sql SQL query text
     * @return RAW result from server
     */
    open fun executeSelectQuery(sql: String): Any? {
        return null
    }

    /**
     * Method used to transform RAW result of SELECT query returned by server to array of rows
     * @param result RAW result from database server
     * @param collectionName Name of collection queried, or null, if it was multi-table query
     * @return Result as array of rows
     */
    fun processQueryResult(result: Any?, collectionName: String): ArrayList<HashMap<String, Any>>? {
        val rawRows = parseQueryResult(result)
        if (rawRows == null || rawRows!!.size == 0) return null
        val resultRows = ArrayList<HashMap<String, Any>>()
        for (rawRow in rawRows!!) {
            val resultRow = processQueryResultRow(rawRow, collectionName)
            if (resultRow.size > 0) resultRows.add(resultRow)
        }
        return resultRows
    }

    /**
     * Method used to transform RAW query result to array of rows (without transofrming field values)
     * @param result Query result to transform
     * @return
     */
    open fun parseQueryResult(result: Any?): ArrayList<Map<String, Any>>? {
        return null
    }

    /**
     * Method used to transform RAW row returned from database server to HashMap
     * @param rawRow Raw row from server
     * @param collectionName Collection to which this row belongs or nothing, if its unknown
     * @return Row with fields, transformed to appropriate format
     */
    fun processQueryResultRow(rawRow: Map<String, Any>, collectionName: String?): HashMap<String, Any> {
        val resultRow = HashMap<String, Any>()
        for (key in rawRow.keys) {
            var value: Any? = rawRow[key]
            if (collectionName != null)
                value = formatFieldValue(collectionName, key, value)
            if (value != null) resultRow[key] = value
        }
        return resultRow
    }

    /**
     * Base method, used to insert or update data in database
     * @param collectionName Name of collection to update
     * @param data Array of records
     * @param isNew If true, then "INSERT" data, if false then "UPDATE" data
     * @return Number of affected records
     */
    abstract fun processUpdateQuery(collectionName: String, data: ArrayList<HashMap<String, Any>>, isNew: Boolean): Int?

    /**
     * Utility Method checks if provided collection field has correct configuration
     * @param collectionName Name of collection
     * @param fieldName Field name
     * @return True if this field configured correctly or false otherwise
     */
    fun isValidFieldConfig(collectionName: String, fieldName: String): Boolean {
        if (getFieldConfigValue(collectionName, fieldName, "name") == null) return false
        return if (getFieldConfigValue(collectionName, fieldName, "type") == null) false else true
    }

    /**
     * Utility method which returns value of specified config parameter of specified field
     * @param collectionName Name of collection
     * @param fieldName Field name
     * @param variable Configuration key
     * @return Configuration value
     */
    fun getFieldConfigValue(collectionName: String, fieldName: String, variable: String): Any? {
        val field = getFieldConfig(collectionName, fieldName) ?: return null
        return if (field!!.containsKey(variable)) field!![variable] else null
    }

    /**
     * Returns configuration object with all configuration parameters for specified field from config file
     * @param collectionName Name of collection
     * @param fieldName Name of field
     * @return Configuration object as HashMap
     */
    fun getFieldConfig(collectionName: String, fieldName: String): HashMap<String, Any>? {
        val fields = getCollectionFieldsConfig(collectionName)
        return if (fields == null || !(fields is HashMap<*, *>) || !fields!!.containsKey(fieldName)) null else fields!!.get(fieldName) as HashMap<String, Any>
    }

    /**
     * Method returns path of Collection configuration, related to fields of collection
     * @param collectionName Name of collection
     * @return HashMap with config related to fields
     */
    fun getCollectionFieldsConfig(collectionName: String): HashMap<String, Any>? {
        val collection = getCollectionConfig(collectionName)
        return if ((collection == null || !collection!!.containsKey("fields") ||
                        !(collection!!.get("fields") is HashMap<*, *>))) null else collection!!.get("fields") as HashMap<String, Any>
    }

    /**
     * Method returns list of field names in collection
     * @param collectionName Name of collection
     * @return Set of feild names
     */
    fun getCollectionFields(collectionName: String): Set<String> {
        val fields = getCollectionFieldsConfig(collectionName)
    //  result.remove(getIdFieldName(collectionName));
        return fields!!.keys
    }

    /**
     * Returns configuration object for specified collection, which includes configuration for whole collection
     * and for each field inside it
     * @param collectionName Name of collection
     * @return Configuration object as HashMap
     */
    fun getCollectionConfig(collectionName: String): HashMap<String, Any>? {
        return if (!this.collections.containsKey(collectionName)) null else collections.get(collectionName) as HashMap<String, Any>
    }

    /**
     * Returns name of field, which specified collection uses as ID field (as specified in configuration file)
     * @param collectionName Name of collection
     * @return Name of field
     */
    fun getIdFieldName(collectionName: String): String? {
        val collection = getCollectionConfig(collectionName)
        if (collection == null || !collection!!.containsKey("idField")) return null
        val indexField = collection!!["idField"].toString()
        if (!collection!!.containsKey("fields")) return null
        val fields = collection!!["fields"] as HashMap<String, Any>
        return if (!fields.containsKey(indexField)) null else indexField
    }

    /**
     * Formats value for specified field for UPDATE or INSERT query, depending on type of this field, defined
     * in configuration file
     * @param collectionName Name of collection
     * @param fieldName Name of field
     * @param value Value of field to format
     * @return Properly formatted and escaped value to insert to SQL query line
     */
    fun formatFieldValue(collectionName: String, fieldName: String, value: Any?): Any? {
        if (!isValidFieldConfig(collectionName, fieldName)) return null
        if (value == null) return null
        val type = getFieldConfigValue(collectionName, fieldName, "type")!!.toString()
        try {
            when (type) {
                "decimal" -> return java.lang.Double.valueOf(value!!.toString())
                "integer" -> return java.lang.Double.valueOf(value!!.toString()).toInt()
                "string" -> return value!!.toString()
            }
        } catch (e: Exception) {
            syslog.log(ISyslog.LogLevel.WARNING,
                    "Could not format field value '" + value + "' of field '" + fieldName + "'" +
                            "in collection '" + collectionName + "'",
                    this.javaClass.name, "formatFieldValue")
        }

        return null
    }


    companion object {

        /**
         * Factory method which returns concrete data adapter by unique name, using configuration file
         * @param name Name of adapter, defined in configuration file
         * @return Configured object
         */
        operator fun get(name: String): IDatabaseAdapter? {
            val config = ConfigManager.getInstance().getDatabaseAdapter(name)
            if (config == null || !config!!.containsKey("type")) return null
            var result: IDatabaseAdapter? = null
            when (config!!["type"].toString()) {
                "mysql" -> result = MysqlDatabaseAdapter()
                "sqlite" -> result = SqliteDatabaseAdapter()
                "orientdb" -> result = OrientDBDatabaseAdapter()
                "mongodb" -> result = MongoDatabaseAdapter()
            }
            if (result != null) result!!.configure(config!!)
            return result
        }
    }

}
