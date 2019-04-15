package loggers.parsers

import main.ISyslog
import java.util.HashMap

/**
 * Base interface which all content parsers must implement, to be used by loggers.
 */
interface IParser {
    var syslog:ISyslog
    var inputString:String
    fun parse(): HashMap<String, *>
    fun configure(config: HashMap<String, Any>)
}