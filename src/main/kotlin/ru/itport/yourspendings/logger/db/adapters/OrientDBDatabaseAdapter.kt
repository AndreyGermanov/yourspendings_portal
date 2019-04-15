package db.adapters

import com.google.gson.Gson
import com.mashape.unirest.http.Unirest
import java.io.BufferedReader
import java.io.InputStreamReader
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

/**
 * Database adapter for OrientDB databases
 */
class OrientDBDatabaseAdapter : JDBCDatabaseAdapter() {

    // Connection credentials

    private var host = ""
    private var port = ""
    private var username = ""
    private var password = ""
    private var database = ""
    private var mode = WorkMode.jdbc
    private val gson = Gson()

    /**
     * Method used to apply configuration to data adapter
     * @param config Configuration object
     */
    override fun configure(config: HashMap<String, Any>) {
        super.configure(config)
        if (config == null) return
        this.host = (config as java.util.Map<String, Any>).getOrDefault("host", "").toString()
        this.port = Integer.toString(java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("port", "").toString()).toInt())
        this.username = (config as java.util.Map<String, Any>).getOrDefault("username", "").toString()
        this.password = (config as java.util.Map<String, Any>).getOrDefault("password", "").toString()
        this.database = (config as java.util.Map<String, Any>).getOrDefault("database", "").toString()
        try {
            this.mode = WorkMode.valueOf((config as java.util.Map<String, Any>).getOrDefault("mode", "jdbc").toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Method used to open database connection (which is previously setup adn configured)
     */
    override fun connect() {
        if (this.mode != WorkMode.jdbc) return
        val url = "jdbc:orient:remote:$host:$port/$database"
        val info = Properties()
        info["user"] = username
        info["password"] = password
        try {
            this.connection = DriverManager.getConnection(url, info)
        } catch (e: SQLException) {
            syslog.logException(e, this, "connect")
        }

    }

    /**
     * Base method, used to insert or update data in database
     * @param collectionName Name of collection to update
     * @param data Array of records
     * @param isNew If true, then "INSERT" data, if false then "UPDATE" data
     * @return Number of affected records
     */
    override fun processUpdateQuery(collectionName: String, data: ArrayList<HashMap<String, Any>>, isNew: Boolean): Int {
        if (mode == WorkMode.jdbc) return super.processUpdateQuery(collectionName, data, isNew)
        val updateStatement = prepareUpdateBatchSQL(collectionName, data, isNew)
        return if (updateStatement.isEmpty()) 0 else executeUpdateQuery(updateStatement)
    }

    /**
     * Method used to execute specified update query in database and return number of affected rows
     * @param updateStatement Query statement to execute
     * @return Number of affected rows
     */
    override fun executeUpdateQuery(updateStatement: Any): Int {
        if (mode == WorkMode.jdbc) return executeUpdateQuery(updateStatement)
        val options = hashMapOf("batch" to  true) as HashMap<String,Any>
        val result = execOrientDBRequest(updateStatement.toString(), options)
        if (result.isEmpty()) return 0
        val resultJson = gson.fromJson(result, HashMap::class.java)
        return if (!resultJson.containsKey("result")) 0 else ((resultJson as HashMap<String,Any>).getOrDefault("result", ArrayList<String>()) as ArrayList<*>).size
    }

    /**
     * Method used to prepare set of SQL queries to UPDATE or INSERT multiple records to database
     * @param collectionName Name of collection to update
     * @param data Array or data rows to UPDATE or INSERT
     * @param isNew if true, then method will return INSERT queries, otherwise will return UPDATE queries
     * @return Set of INSERT or UPDATE query lines, delimited by ';' symbol
     */
    override fun prepareUpdateBatchSQL(collectionName: String, data: ArrayList<HashMap<String, Any>>, isNew: Boolean): String {
        if (mode == WorkMode.jdbc) return super.prepareUpdateBatchSQL(collectionName, data, isNew)
        if (data.size == 0) return ""
        val keys = getCollectionFields(collectionName).stream()
                .filter { key -> !key.equals(getIdFieldName(collectionName)) }
                .reduce { s, s1 -> "$s,$s1" }.orElse("")
        return (if (isNew) "INSERT INTO $collectionName ($keys) VALUES (" else "") +
                joinSqlLines(collectionName, data, isNew, if (isNew) "),(" else ";") + if (isNew) ")" else ""
    }

    /**
     * Returns INSERT or UPDATE query statement for provided data row
     * @param collectionName Name of collection to update
     * @param row Row which is a set of fields
     * @param isNew if true, then method will return INSERT query, otherwise will return UPDATE query
     * @return
     */
    override fun prepareUpdateSQL(collectionName: String, row: HashMap<String, Any>, isNew: Boolean): String {
        if (mode == WorkMode.jdbc) super.prepareUpdateSQL(collectionName, row, isNew)
        val fields = prepareDataForSql(collectionName, row)
        if (fields.isEmpty()) return ""
        val keys = getCollectionFields(collectionName)
        if (isNew)
            return keys.stream()
                    .filter { key -> key != getIdFieldName(collectionName) }
                    .map { key -> (fields as java.util.Map<String, String>).getOrDefault(key, "null") }
                    .reduce { s, s1 -> "$s,$s1" }.orElse("")
        val fieldString = fields.keys.stream().reduce { s, s1 -> s + "=" + fields[s1] }.orElse("")
        val idValue = formatFieldValueForSQL(collectionName, getIdFieldName(collectionName)!!,
                row[getIdFieldName(collectionName)])
        return if (fieldString.isEmpty() || idValue == null) "" else "UPDATE $collectionName SET $fieldString WHERE id=$idValue"
    }

    /**
     * Method used to execute REST request to OrientDB database server
     * @param sql SQL query to send to server
     * @param options Various request options. (batch mode or single insert mode and others)
     * @return Response from server after request as a string
     */
    fun execOrientDBRequest(sql: String, options: HashMap<String, Any>?): String {
        try {
            val request = preparesOrientDBRequest(sql, options)
            return BufferedReader(
                    InputStreamReader(Unirest.post(request.url).basicAuth(username, password)
                            .header("Accept-Encoding", "gzip,deflate")
                            .body(request.body).asString().rawBody)).lines().reduce { s, s1 -> s + "\n" + s1 }.orElse("")
        } catch (e: Exception) {
            e.printStackTrace()
            syslog.logException(e, this, "execOrientDBRequest")
            return ""
        }

    }

    /**
     * Prepares REST Request to OrientDB server according to input options
     * @param sql SQL text of query
     * @param options Various request options. (batch mode or single insert mode and others)
     * @return Object with 2 fields: "url" - Request URL and "body" - POST body of request
     */
    fun preparesOrientDBRequest(sql: String, options: HashMap<String, Any>?): OrientDBRequest {
        val result = OrientDBRequest("$host:$port", sql)
        var command = "/command/$database/sql"
        if (options == null || options.size == 0) {
            result.url += command
            return result
        }
        val request: HashMap<String, Any>
        if (java.lang.Boolean.parseBoolean((options as java.util.Map<String, Any>).getOrDefault("batch", true).toString())) {
            request = hashMapOf("transaction" to false, "operations" to
                    arrayListOf(hashMapOf("type" to "script", "language" to "sql", "script" to sql))
            )
            command = "/batch/$database"
            result.body = gson.toJson(request)
        }
        result.url += command
        return result
    }

    /**
     * Databases specific method to send SELECT query to server and return RAW result
     * @param sql SQL query text
     * @return RAW result from server
     */
    override fun executeSelectQuery(sql: String): Any? {
        if (mode == WorkMode.jdbc) return super.executeSelectQuery(sql)
        try {
            val result = execOrientDBRequest(sql, null)
            return if (result.isEmpty()) null else result
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    /**
     * Method used to transform RAW query result to array of rows (without transofrming field values)
     * @param result Query result to transform
     * @return
     */
    override fun parseQueryResult(result: Any?): ArrayList<Map<String, Any>>? {
        if (result == null) return null
        val resultJson = gson.fromJson(result.toString(), HashMap::class.java)
        return if (resultJson == null || !resultJson.containsKey("result")) null else resultJson["result"] as ArrayList<Map<String, Any>>
    }

    // Possible OrientDB server communication modes: JDBC or REST
    enum class WorkMode {
        jdbc, rest
    }

    /**
     * Object represents Request to OrientDB server in REST mode
     */
    inner class OrientDBRequest// Class contructor
        constructor(// Request URL
            internal var url: String, // POST body
            internal var body: String) {
        // Returns object as a String
        override fun toString(): String {
            return "URL: $url,BODY=$body"
        }
    }
}