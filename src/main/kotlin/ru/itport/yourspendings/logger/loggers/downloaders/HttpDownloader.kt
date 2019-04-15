package loggers.downloaders

import javax.net.ssl.HttpsURLConnection
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * Base class of data loader over HTTP. All descendants, which get data from websites extend
 * it
 */
abstract class HttpDownloader : Downloader() {

    /// Base URL of web site
    /**
     * Used to get curret base URL
     * @return
     */
    /**
     * Used to manually set base url string
     * @param url
     */
    open var url = ""

    /**
     * Method used to construct URL to download data from
     * @return constructed URL or null if impossible to create valid URL from source parts
     */
    protected val connectionUrl: URL?
        get() {
            try {
                return URL(this.url)
            } catch (e: Exception) {
                this.syslog.logException(e, this, "getConnectionUrl")
                return null
            }

        }

    /**
     * Method creates and initiates connection to source web page
     * @return Connection object or null in case of errors or exceptions
     */
    private fun connect(): HttpURLConnection? {
        val connection: HttpURLConnection
        val url = connectionUrl
        try {
            if (url!!.protocol == "https")
                connection = url.openConnection() as HttpsURLConnection
            else
                connection = url.openConnection() as HttpURLConnection
            connection.connect()
            return connection
        } catch (e: IOException) {
            this.syslog.logException(e, this, "connect")
        }

        return null
    }

    /**
     * Method downloads data using opened connection to URL
     * @return Content as a string
     */
    override fun download(): String {
        var result = ""
        val connection = connect() ?: return result
        try {
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                result = reader.lines().reduce("") { prevResult, line -> prevResult + line }
                connection.disconnect()
            }
        } catch (e: Exception) {
            this.syslog.logException(e, this, "download")
        }

        return result
    }
}
