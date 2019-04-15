package loggers.parsers

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import java.util.HashMap

/**
 * Functional interface for lambda functions, used to parse fields in HTML text
 */
internal interface ParseJsonFunction {
    fun apply(fieldName: String, inputJson: HashMap<String, Any>?): String
}

/**
 * Parser, used to parse JSON strings as an input. All concrete parsers of JSON strings extends it
 */
abstract class JsonParser : Parser() {

    // Input string to parse, transformed to HashMap
    private var inputJson: HashMap<String, Any>? = null

    override fun parse(): HashMap<String, *> {
        initFields()
        val gson = Gson()
        inputJson = gson.fromJson(inputString, HashMap::class.java) as HashMap<String,Any>
        return parseFields()
    }

    /**
     * Method, which descendants use to init configuration of fields, which parser should extract
     * from input string
     */
    abstract override fun initFields()

    internal fun parseFields(): HashMap<String, *> {
        val result = HashMap<String, Any>()
        for (key in fieldDefs.keys) {
            val field = fieldDefs[key]
            putField(field!!, result)
        }
        return result
    }

    /**
     * Method used to get specified field from inputJSON and put it to resulting record
     * @param field Name of field
     * @param result Link to resulting Hashmap to which put this field
     */
    internal fun putField(field: HashMap<String, Any>, result: HashMap<String, Any>) {
        val rawResult = getRawFieldValue(field) ?: return
        val fieldName = field["name"].toString()
        when (field["type"].toString()) {
            "string" -> result[fieldName] = rawResult
            "integer" -> result[fieldName] = java.lang.Long.parseLong(rawResult)
            "decimal" -> result[fieldName] = java.lang.Double.parseDouble(rawResult)
            "boolean" -> result[fieldName] = java.lang.Boolean.parseBoolean(rawResult)
        }
    }

    /**
     * Method used to get RAW string value of field from JSON object
     * @param field Field definition object for field (from fieldDefs)
     * @return Extracted, parsed and transformed field as String
     */
    private fun getRawFieldValue(field: HashMap<String, Any>): String? {
        return if (field["parseFunction"] !is ParseJsonFunction) null else (field["parseFunction"] as ParseJsonFunction).apply(field["name"].toString(), inputJson)
    }

    /**
     * Standard methods, used to parse fields of different types. Used as lambdas to parse fields from JSON object
     * @param fieldName Name of field
     * @param inputJson Source JSON object
     * @return Field value as string
     */

    protected fun parseDoubleField(fieldName: String, inputJson: LinkedTreeMap<String, Any>): String? {
        val value = parseField(fieldName, inputJson) ?: return null
        try {
            return java.lang.Double.valueOf(inputJson[fieldName].toString()).toString()
        } catch (e: NumberFormatException) {
            return null
        }

    }

    protected fun parseIntegerField(fieldName: String, inputJson: LinkedTreeMap<String, Any>): String? {
        val value = parseField(fieldName, inputJson) ?: return null
        try {
            return Integer.valueOf(inputJson[fieldName].toString()).toString()
        } catch (e: NumberFormatException) {
            return null
        }

    }

    protected fun parseLongField(fieldName: String, inputJson: LinkedTreeMap<String, Any>): String? {
        val value = parseField(fieldName, inputJson) ?: return null
        try {
            return java.lang.Long.valueOf(inputJson[fieldName].toString()).toString()
        } catch (e: NumberFormatException) {
            return null
        }

    }

    protected fun parseBooleanField(fieldName: String, inputJson: LinkedTreeMap<String, Any>): String? {
        val value = parseField(fieldName, inputJson) ?: return null
        try {
            return java.lang.Boolean.valueOf(inputJson[fieldName].toString()).toString()
        } catch (e: NumberFormatException) {
            return null
        }

    }

    /**
     * Internal method used to extract string representation value of field
     * @param fieldName Name of field
     * @param inputJson Source JSON object
     * @return Field value as string
     */
    private fun parseField(fieldName: String, inputJson: LinkedTreeMap<String, Any>): String? {
        return if (!inputJson.containsKey(fieldName) || inputJson[fieldName].toString().isEmpty()) null else inputJson[fieldName].toString()
    }
}