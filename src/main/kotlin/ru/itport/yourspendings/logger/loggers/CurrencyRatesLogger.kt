package loggers

import loggers.downloaders.CurrencyRatesDownloader
import loggers.parsers.CurrencyRatesParser

import java.util.HashMap

/**
 * Logger of Currency rates which uses https://ratesapi.io/api/latest endpoint to get data
 */
class CurrencyRatesLogger
/**
 * Class constructor
 * @param config Configuration object
 */
(config: HashMap<String, Any>) : Logger(config) {

    init {
        this.downloader = CurrencyRatesDownloader()
        this.parser = CurrencyRatesParser()
        this.propagateSyslog()
        this.configure(config)
    }
}
