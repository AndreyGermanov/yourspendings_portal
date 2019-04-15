package loggers.downloaders

import java.util.HashMap

/**
 * Downloaded for CurrencyRatesLogger
 */
class CurrencyRatesDownloader : HttpDownloader() {

    override var url = "https://ratesapi.io/api/latest"
    private var base = "USD"

    override fun configure(config: HashMap<String, Any>) {
        super.configure(config)
        base = (config as java.util.Map<String, Any>).getOrDefault("base", base).toString()
    }

    fun getURL() = url


}
