package main

import java.util.HashMap

interface ISyslog {
    fun logException(e: Exception, source: Any, methodName: String)

    fun log(level: LogLevel, message: String, className: String, methodName: String)

    /**
     * Possible log levels
     */
    enum class LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }

    /**
     * Interface which class must implement to be able to use this object to log messages
     */
    interface Loggable {
        val name: String
        val syslogPath: String
        val syslogConfig: HashMap<String, Any>
    }
}