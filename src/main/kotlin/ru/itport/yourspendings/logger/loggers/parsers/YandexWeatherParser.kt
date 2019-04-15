package loggers.parsers

import java.util.HashMap

/**
 * Parser for "Yandex Weather" logger. Used to parse Weather information, downloaded from Yandex weather site.
 */
class YandexWeatherParser
/**
 * Class constructor
 * @param placeName Place to get data for
 */
(placeName: String) : HTMLParser(placeName) {

    /**
     * Method defines regular expressions which used to find field values in input HTML
     * @return HashMap with regular expressions keyed by field names
     */
    override val regEx: HashMap<String, String>
        get() = hashMapOf(
                "temperature" to "<div class=\"temp fact__temp fact__temp_size_s\"><span class=\"temp__value\">" +
                ".*?([0-9\\.\\,]*)</span><span class=\"temp__unit i-font " +
                "i-font_face_yandex-sans-text-medium\">°</span></div>",
                "water_temperature" to "<div class=\"temp fact__water-temp\">" +
                "<span class=\"temp__value\">([0-9\\.\\,]*)</span>" +
                "<span class=\"temp__unit i-font i-font_face_yandex-sans-text-medium\">°</span></div>",
                "humidity" to "<dd class=\"term__value\"><i class=\"icon icon_humidity-white term__fact-icon\"></i>([0-9\\.\\,]*)</dd>",
                "pressure" to "<dd class=\"term__value\"><i class=\"icon icon_pressure-white term__fact-icon\">" + "</i>([0-9\\.\\,]*) <span class=\"fact__unit\">мм рт. ст.</span></dd>",
                "wind_speed" to "<span class=\"wind-speed\">([0-9\\.\\,]*)</span>",
                "wind_direction" to "<span class=\"fact__unit\">м/с, <abbr class=\" icon-abbr\" " + "title=\"Ветер: .*?\">(.*)</abbr>"
        )

    /**
     * Method, which descendants use to init configuration of fields, which parser should extract
     * from input string
     */
    override fun initFields() {
        val parseFunc =  this::parseField
        fieldDefs = hashMapOf(
                "temperature" to hashMapOf("name" to "temperature", "type" to Double::class.java, "parseFunction" to parseFunc),
                "water_temperature" to hashMapOf("name" to "water_temperature", "type" to Double::class.java, "parseFunction" to parseFunc),
                "humidity" to hashMapOf("name" to "humidity", "type" to Double::class.java, "parseFunction" to parseFunc),
                "pressure" to hashMapOf("name" to "pressure", "type" to Double::class.java, "parseFunction" to parseFunc),
                "wind_speed" to hashMapOf("name" to "wind_speed", "type" to Double::class.java, "parseFunction" to parseFunc),
                "wind_direction" to hashMapOf("name" to "wind_direction", "type" to String::class.java, "parseFunction" to parseFunc)
        )
    }
}