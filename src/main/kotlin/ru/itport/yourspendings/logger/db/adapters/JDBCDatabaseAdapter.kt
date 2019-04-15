package db.adapters

import main.ISyslog

import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import java.util.Optional
import java.util.stream.Collectors

/**
 * Base class for all Database adapters, which is based on JDBC interface
 */
abstract class JDBCDatabaseAdapter : DatabaseAdapter() {

    // Link to database connection
    protected var connection: Connection? = null

    /**
     * Method used to open database connection (which is previously setup adn configured)
     */
    internal abstract fun connect()

    /**
     * Base method, used to insert or update data in database
     * @param collectionName Name of collection to update
     * @param data Array of records
     * @param isNew If true, then "INSERT" data, if false then "UPDATE" data
     * @return Number of affected records
     */
    override fun processUpdateQuery(collectionName: String, data: ArrayList<HashMap<String, Any>>, isNew: Boolean): Int {
        if (connection == null) this.connect()
        if (connection == null) return 0
        val updateStatement = prepareUpdateBatchSQL(collectionName, data, isNew)
        return if (updateStatement.isEmpty()) 0 else executeUpdateQuery(updateStatement)!!
    }

    /**
     * Method used to execute specified update query in database and return number of affected rows
     * @param updateStatement Query statement to execute
     * @return Number of affected rows
     */
    open fun executeUpdateQuery(updateStatement: Any): Int? {
        val updateString = updateStatement.toString()
        val statement: Statement
        try {
            statement = connection!!.createStatement()
            Arrays.stream(updateString.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).forEach { sql ->
                try {
                    syslog.log(ISyslog.LogLevel.DEBUG, "Adding SQL to batch: '$sql'", this.javaClass.name, "executeUpdateQuery")
                    statement.addBatch(sql)
                } catch (e: Exception) {
                    syslog.logException(e, this, "executeUpdateQuery")
                }
            }
            syslog.log(ISyslog.LogLevel.DEBUG, "Executing SQL batch", this.javaClass.name, "executeUpdateQuery")
            return Arrays.stream(statement.executeBatch()).reduce { i, i1 -> i + i1 }.orElse(0)
        } catch (e: Exception) {
            syslog.log(ISyslog.LogLevel.ERROR, "Could not execute batch query '$updateString'",
                    this.javaClass.name, "executeUpdateQuery")
            return null
        }

    }

    /**
     * Method used to prepare set of SQL queries to UPDATE or INSERT multiple records to database
     * @param collectionName Name of collection to update
     * @param data Array or data rows to UPDATE or INSERT
     * @param isNew if true, then method will return INSERT queries, otherwise will return UPDATE queries
     * @return Set of INSERT or UPDATE query lines, delimited by ';' symbol
     */
    open fun prepareUpdateBatchSQL(collectionName: String, data: ArrayList<HashMap<String, Any>>, isNew: Boolean): String {
        return joinSqlLines(collectionName, data, isNew, ";")
    }

    /**
     * Method gets list of data rows to insert or update and returns concatentated
     * string of appropriate UPDATE or INSERT statements
     * @param collectionName Name of colleciton
     * @param data Data array
     * @param isNew Is it new rows (INSERT) or not (UPDATE)
     * @param delimiter - Delimiter of SQL statements (usually ';')
     * @return String with SQL statements delimited by delimiter
     */
    fun joinSqlLines(collectionName: String, data: ArrayList<HashMap<String, Any>>, isNew: Boolean, delimiter: String): String {
        return data.stream()
                .filter { row -> row.size > 0 }
                .map { row -> prepareUpdateSQL(collectionName, row, isNew) }
                .filter { string -> string != null && !string.isEmpty() }
                .reduce { s, s1 -> s + delimiter + s1 }.orElse("")
    }

    /**
     * Returns INSERT or UPDATE query statement for provided data row
     * @param collectionName Name of collection to update
     * @param row Row which is a set of fields
     * @param isNew if true, then method will return INSERT query, otherwise will return UPDATE query
     * @return
     */
    open fun prepareUpdateSQL(collectionName: String, row: HashMap<String, Any>, isNew: Boolean): String {
        val fields = prepareDataForSql(collectionName, row)
        if (isNew) {
            val keys = fields.keys.stream().reduce { s, s1 -> "$s,$s1" }.orElse("")
            val values = fields.values.stream().reduce { s, s1 -> "$s,$s1" }.orElse("")
            return if (keys.isEmpty()) "" else "INSERT INTO $collectionName ($keys) VALUES($values)"
        }
        val fieldString = fields.keys.stream().reduce { s, s1 -> s + "=" + fields[s1] }.orElse("")
        val idValue = formatFieldValueForSQL(collectionName, getIdFieldName(collectionName) ?: "",
                row[getIdFieldName(collectionName)])
        return if (fieldString.isEmpty() || idValue == null) "" else "UPDATE $collectionName SET $fieldString WHERE id=$idValue"
    }

    /**
     * Method returns row of fields, formatted according to configuration and ready to be used in
     * SQL statements
     * @param collectionName Name of collection
     * @param row Row of data
     * @return Row of data with field values, formatted according to their types
     */
    fun prepareDataForSql(collectionName: String, row: HashMap<String, Any>): HashMap<String, String> {
        return row.keys.stream()
                .filter { fieldName -> formatFieldValueForSQL(collectionName, fieldName, row[fieldName]) != null }
                .collect(Collectors.toMap({ fieldName -> fieldName },
                        { fieldName -> formatFieldValueForSQL(collectionName, fieldName, row[fieldName]) },
                        { s1, s2 -> s1 },{ HashMap<String,String>() }
                ))
    }

    /**
     * Formats value for specified field for UPDATE or INSERT query, depending on type of this field, defined
     * in configuration file
     * @param collectionName Name of collection
     * @param fieldName Name of field
     * @param value Value of field to format
     * @return Properly formatted and escaped value to insert to SQL query line
     */
     fun formatFieldValueForSQL(collectionName: String, fieldName: String, value: Any?): String? {
        if (!isValidFieldConfig(collectionName, fieldName)) return null
        if (value == null) return null
        val type = getFieldConfigValue(collectionName, fieldName, "type").toString()
        when (type) {
            "decimal" -> return value.toString()
            "integer" -> return value.toString()
            "string" -> return "'" + value.toString() + "'"
        }
        return null
    }
}