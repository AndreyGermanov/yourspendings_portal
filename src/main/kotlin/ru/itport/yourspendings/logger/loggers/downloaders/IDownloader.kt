package loggers.downloaders

import main.ISyslog

import java.util.HashMap

/**
 * Base interface that all data loader classes must implement to be used in Data loggers
 */
interface IDownloader {
    fun download(): String
    var syslog: ISyslog
    fun configure(config: HashMap<String, Any>)
}