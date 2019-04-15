package loggers

import loggers.downloaders.YandexWeatherDownloader
import loggers.parsers.YandexWeatherParser
import java.util.HashMap

/**
 * Logger used to log weather information from Yandex website. It allows to setup place to get data for.
 * Logged information includes temperature, water temperature, humidity, wind speed and direction.
 */
internal class YandexWeatherLogger : Logger {

    /**
     * Class constructors
     */
    constructor(name: String, placeName: String) : super(name) {
        this.parser = YandexWeatherParser("")
        this.downloader = YandexWeatherDownloader(placeName)
        this.propagateSyslog()
    }

    constructor(config: HashMap<String, Any>) : super(config) {
        this.downloader = YandexWeatherDownloader("")
        this.parser = YandexWeatherParser("")
        this.propagateSyslog()
        this.configure(config)
    }
}