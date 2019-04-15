package loggers.parsers

import main.ISyslog

import java.util.HashMap

/**
 * Basic class for Data Parser. Used to parse downloaded data and transform it to HashMap with fields and their values
 */
abstract class Parser : IParser {

    /// Input string, which parser processes to extract data
    override var inputString: String = ""

    /// Field definitions. Contains information about fields, which parser should extract from inputString
    /// and about rules used to extract them
    protected var fieldDefs = HashMap<String, HashMap<String, Any>>()

    /// During processing, parser can experience errors or throw exceptions. This is link to Syslog object,
    /// used to write this to log file
    override lateinit var syslog: ISyslog

    abstract fun initFields()

    /**
     * Main method used to parse record.
     * @return HashMap<String></String>,?>: Hashmap with all extracted fields and their values
     */
    abstract override fun parse(): HashMap<String, *>

    override fun configure(config: HashMap<String, Any>) {}

}