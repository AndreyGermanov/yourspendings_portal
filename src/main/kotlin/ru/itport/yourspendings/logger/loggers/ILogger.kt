package loggers

import cronjobs.ICronjobTask
import main.ISyslog
import java.util.HashMap

/**
 * Interface which all data logger classes must implement to be loaded by LoggerService
 */
interface ILogger : ICronjobTask {
    fun log()
    fun propagateSyslog()
}