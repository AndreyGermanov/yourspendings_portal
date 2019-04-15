package db.adapters

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.InsertOneModel
import com.mongodb.client.model.WriteModel
import main.ISyslog
import org.bson.Document
import java.util.ArrayList
import java.util.HashMap
import java.util.stream.Collectors

/**
 * Database adapter for MongoDB database
 */
class MongoDatabaseAdapter : DatabaseAdapter() {

    // Link to database connection
    private var connection: MongoClient? = null
    // Database Host name
    private var host = "localhost"
    // Database port
    private var port: Int? = 27017
    // Name of database
    private var database = ""
    // Database object to communicate with
    private var db: MongoDatabase? = null

    /**
     * Method used to apply configuration to data adapter
     * @param config Configuration object
     */
    override fun configure(config: HashMap<String, Any>) {
        super.configure(config)
        this.host = (config as java.util.Map<String, Any>).getOrDefault("host", this.host).toString()
        this.port = java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("port", this.port).toString()).toInt()
        this.database = (config as java.util.Map<String, Any>).getOrDefault("database", this.database).toString()
    }

    /**
     * Method used to open database connection, based on currect configuration
     */
    fun connect() {
        try {
            this.connection = MongoClients.create("mongodb://" + this.host + ":" + this.port)
            this.db = this.connection!!.getDatabase(this.database)
        } catch (e: Exception) {
            this.connection = null
            syslog.logException(e, this, "connect")
        }

    }

    /**
     * Base method for UPDATE and INSERT database queries
     * @param collectionName Name of collection to update
     * @param data Array of records
     * @param isNew If true, then "INSERT" data, if false then "UPDATE" data
     * @return Number of affected records
     */
    override fun processUpdateQuery(collectionName: String, data: ArrayList<HashMap<String, Any>>, isNew: Boolean): Int {
        if (connection == null) this.connect()
        if (connection == null) {
            syslog.log(ISyslog.LogLevel.ERROR, "Could not connect to database",
                    this.javaClass.name, "processUpdateQuery")
            return 0
        }
        return executeUpdateQuery(collectionName, prepareUpdateStatement(collectionName, data))!!
    }

    /**
     * Method which executes specified update query for specified collection in database
     * @param collectionName Name of collection
     * @param updateStatement Prepared query statement to execute
     * @return Number of affected records
     */
    internal fun executeUpdateQuery(collectionName: String, updateStatement: List<WriteModel<Document>>): Int? {
        try {
            val result = db!!.getCollection(collectionName).bulkWrite(updateStatement)
            return result.getModifiedCount() + result.getInsertedCount()
        } catch (e: Exception) {
            syslog.logException(e, this, "executeUpdateQuery")
            return null
        }

    }

    /**
     * Method used to prepare list of database queries to update specified array of rows in database
     * @param collectionName Name of collection to update
     * @param data Array of rows to update
     * @return Array of prepared query statement
     */
    fun prepareUpdateStatement(collectionName: String, data: ArrayList<HashMap<String, Any>>): List<WriteModel<Document>> {
        return data.stream()
                .map<Any> { row -> prepareInsertDocumentStatement(collectionName, row) }
                .collect(Collectors.toList()) as List<WriteModel<Document>>
    }

    /**
     * Utility method used to prepare database update query statement for specified data row
     * @param collectionName Name of collection to update
     * @param row Data row
     * @return Prepared query statement
     */
    internal fun prepareInsertDocumentStatement(collectionName: String, row: HashMap<String, Any>): WriteModel<Document> {
        val result = Document()
        row.keys.forEach { fieldName ->
            val value = formatFieldValue(collectionName, fieldName, row[fieldName])
            if (value != null) result.append(fieldName, value)
        }
        return InsertOneModel(result)
    }

    override fun select(sql: String, collectionName: String?): ArrayList<HashMap<String, Any>> {
        return ArrayList()
    }

}
