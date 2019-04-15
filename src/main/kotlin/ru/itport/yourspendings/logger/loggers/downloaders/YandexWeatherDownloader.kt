package loggers.downloaders

import java.net.MalformedURLException
import java.net.URL
import java.util.HashMap

/**
 * Yandex Weather data loader. Used by Yandex Weather logger to download data about weather.
 */
class YandexWeatherDownloader
/**
 * Class constructor
 * @param placeName Place to get data for
 */
(placeName: String) : HttpDownloader() {

    /// Base URL
    override var url = "https://yandex.ru/pogoda"
    /// Place (city, country etc) of place to get data for
    /**
     * Used to manually get place
     * @return
     */
    /**
     * Used to manually set place
     * @param placeName
     */
    internal var placeName: String? = null
        set

    init {
        this.placeName = placeName
    }

    override fun configure(config: HashMap<String, Any>) {
        super.configure(config)
        placeName = (config as java.util.Map<String, Any>).getOrDefault("place", placeName).toString()
    }

    fun getURL(): String {
        return this.url + "/" + this.placeName
    }

}
