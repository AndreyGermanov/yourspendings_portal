package loggers.downloaders

import main.ISyslog

import java.util.HashMap

/**
 * Base class for all data downloaders.
 */
abstract class Downloader : IDownloader {

    /// During download process, object can experience probems and throw exceptions.
    /// This Syslog object used to write them to log file
    override lateinit var syslog: ISyslog

    /**
     * Main method, which downloader use to get content
     * @return Content as string
     */
    abstract override fun download(): String

    override fun configure(config: HashMap<String, Any>) {}
}
