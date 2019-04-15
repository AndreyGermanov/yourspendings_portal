package loggers.parsers

import main.ISyslog

import java.util.HashMap
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * Functional interface for lambda functions, used to parse fields in HTML text
 */
internal interface ParseHtmlFunction {
    fun apply(fieldName: String, inputString: String): Any?
}

/**
 * Parser class used to extract data from HTML string.
 */
abstract class HTMLParser : Parser {

    /**
     * Method defines regular expressions which used to find field values in input HTML
     * @return HashMap with regular expressions keyed by field names
     */
    open val regEx: HashMap<String, String>
        get() = HashMap()

    /**
     * Class constructors
     */

    internal constructor() {}

    constructor(inputString: String) {
        this.inputString = inputString
        initFields()
    }

    /**
     * Method, which descendants use to init configuration of fields, which parser should extract
     * from input string
     */
    abstract override fun initFields()

    /**
     * Main method used to parse record.
     * @return HashMap of results or empty HashMap
     */
    override fun parse(): HashMap<String, *> {
        return parseFields()
    }

    /**
     * Method which goes over field definitions array and extracts all possible fields and their values
     * from input string
     * @return HashMap with results or empty Hashmap
     */
    internal fun parseFields(): HashMap<String, Any> {
        val result = HashMap<String, Any>()
        for (key in fieldDefs.keys) {
            val field = fieldDefs[key]
            if (!field!!.containsKey("parseFunction") || field!!["parseFunction"] !is ParseHtmlFunction) continue
            val value = (field!!["parseFunction"]!! as ParseHtmlFunction).apply(key, inputString)
            if (value != null) result[key] = value
        }
        return result
    }

    /**
     * Helper method which parses single field from input string and returns result
     * @param fieldName: Name of field to extract
     * @param inputString: Source string, to search field in
     * @return Object with field value. Type of object depends on field type
     */
    internal fun parseField(fieldName: String, inputString: String): Any? {
        val fieldMetadata = fieldDefs[fieldName]
        val regex = getFieldRegex(fieldName)
        val type = fieldMetadata!!["type"] as Class<*>
        try {
            if (type == Double::class.java) {
                return parseDecimalValue(regex, inputString)
            }
            if (type == String::class.java) {
                return parseStringValue(regex, inputString)
            }
        } catch (e: NumberFormatException) {
            this.syslog.log(ISyslog.LogLevel.ERROR, "Field: '" + fieldName + "'. Error: " + e.message + ".",
                    this.javaClass.name, "parseField")
        }

        return null
    }

    /**
     * Helper method used to extract decimal value from input string
     * @param regex: Regular expression used to search
     * @param text: Text used to search in
     * @return Double value or throws exception if not possible to find value of correct type
     */
    internal fun parseDecimalValue(regex: String, text: String): Double? {
        var value = parseValue(regex, text)
        if (value.isEmpty()) throw NumberFormatException("Incorrect value '$value'. Regex used: '$regex'")
        value = value.replace(",", ".")
        return java.lang.Double.valueOf(value)
    }

    /**
     * Helper method used to extract String value from input string
     * @param regex: Regular expression used to search
     * @param text: Text used to search in
     * @return String value or throws exception if not possible to find value of correct type
     */
    internal fun parseStringValue(regex: String, text: String): String {
        val value = parseValue(regex, text)
        if (value.isEmpty()) throw NumberFormatException("Incorrect value '$value'. Regex used: '$regex'")
        return parseValue(regex, text).trim { it <= ' ' }
    }


    /**
     * Helper method used to extract RAW string field value from input string
     * @param regex: Regular expression used to search
     * @param text: Text used to search in
     * @return String value or empty string if not possible to find field in input text
     */
    internal fun parseValue(regex: String, text: String): String {
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(text)
        return if (!matcher.find() || matcher.groupCount() == 0) "" else matcher.group(1)
    }

    /**
     * Method used to get regular expression, used to search and extract value of specified field in input HTML
     * @param fieldName Name of field
     * @return String with regular expression or empty string if regular expression not found
     */
    internal fun getFieldRegex(fieldName: String): String {
        val fieldRegEx = regEx
        return if (!fieldRegEx.containsKey(fieldName)) "" else fieldRegEx[fieldName]!!
    }
}
