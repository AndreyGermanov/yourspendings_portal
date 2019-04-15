package loggers.parsers

import com.google.gson.internal.LinkedTreeMap
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Arrays
import java.util.HashMap

/**
 * Parser for CurrencyRatesLogger
 */
class CurrencyRatesParser : JsonParser() {

    /**
     * Method, which descendants use to init configuration of fields, which parser should extract
     * from input string
     */
    override fun initFields() {
        val parseCurrencyRate =  { currencyName:String, inputJson:HashMap<String, Any> -> this.parseCurrencyRate(currencyName, inputJson) }
        fieldDefs = hashMapOf(
                "timestamp" to hashMapOf("name" to "timestamp", "type" to "integer",
                "parseFunction" to  { fieldName:String, inputJson:HashMap<String, Any> -> this.parseCurrencyTimestamp(fieldName, inputJson) }
        )
        )
        Arrays.asList("AUD", "BGN", "BRL", "CAD", "CHF", "CNY", "CZK", "DKK", "EUR", "GBP", "HKD", "HRK",
                "HUF", "IDR", "ILS", "INR", "ISK", "JPY", "KRW", "MXN", "MYR", "NOK", "NZD", "PHP", "PLN",
                "RON", "RUB", "SEK", "SGD", "THB", "TRY", "ZAR")
                .forEach { key ->
                    fieldDefs[key] = hashMapOf(
                            "name" to key, "inArray" to true, "type" to "decimal", "parseFunction" to parseCurrencyRate
                    )
                }
    }

    /**
     * Method used to parse timestamp field from JSON string
     * @param fieldName Name of field which contains timestamp (or "data" as default)
     * @param inputJson Transformed to JSON input string
     * @return Timestamp converted to string
     */
    private fun parseCurrencyTimestamp(fieldName: String, inputJson: HashMap<String, Any>): String? {
        if (!inputJson.containsKey("date")) return null
        val date = LocalDate.parse(inputJson["date"].toString())
        val datetime = LocalDateTime.of(date, LocalTime.of(0, 0, 0))
        return datetime.toEpochSecond(ZoneOffset.UTC).toString()
    }

    /**
     * Method used to parse and return rate of specified rate from provided JSON object
     * @param currencyName Name of field (currency) to use
     * @param inputJson Transformed to JSON string
     * @return Decimal currency rate converted to string
     */
    private fun parseCurrencyRate(currencyName: String, inputJson: HashMap<String, Any>): String? {
        if (!inputJson.containsKey("rates") || inputJson["rates"] !is LinkedTreeMap<*, *>) return null
        val rates = inputJson["rates"] as LinkedTreeMap<String, Any>
        return parseDoubleField(currencyName, rates)
    }
}