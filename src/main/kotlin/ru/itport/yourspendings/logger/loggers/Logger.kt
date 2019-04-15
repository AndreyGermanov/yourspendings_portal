package loggers

import com.google.gson.Gson
import cronjobs.CronjobTask
import cronjobs.CronjobTaskStatus
import loggers.downloaders.IDownloader
import loggers.parsers.IParser
import main.ISyslog
import main.LoggerApplication
import main.Syslog
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Arrays
import java.util.HashMap
import java.util.stream.Collectors

/**
 * Base class for Data loggers.
 * Each data logger used to download source data using "Data Downloader class", parse it and extract
 * needed fields using "Data parser class" and write to file in filesystem
 */
abstract class Logger
/**
 * Class constuctors
 */
internal constructor() : CronjobTask(), ILogger, Cloneable, ISyslog.Loggable {

    /// ID of logger
    /**
     * Used to get unique name of this logger module
     * @return
     */
    override var name: String = ""

    /// Link to Data downloader class instance
    lateinit var downloader: IDownloader
    /// Link to Data parser class instance
    lateinit var parser: IParser
    /// Link to Syslog object, used to write messages and error exceptions to log file
    /**
     * Used to get link to current syslog object, used to log messages about this logger
     * @return
     */
    /**
     * Used to manually set instance of Syslog object
     * @param syslog
     */
    override var syslog: ISyslog? = null
    /// Last record of data, written
    override var lastRecord: HashMap<String, Any>? = null

    /// Should write duplicate data (if current record is the same as previous (lastRecord)).
    var shouldWriteDuplicates = false
    /// Destination path to which logger will write parsed data
    private var destinationPath = ""
    /// Path, to which logger will write status information, as last record
    override var mstatusPath = ""
    // List of field names, which should be logged to file. If empty or null, then
    // all fields will be saved
    private var fieldsToLog: List<String>? = null

    /**
     * Returns serialized information about last record as a string, ready to write to file in "statusPath"
     * @return String representation of last record or null if not able to produce this string
     */
    override val lastRecordString: String?
        get() = getJson(lastRecord)

    override val lastRecordTimestamp: Long
        get() =
            if (lastRecord == null || !lastRecord!!.containsKey("timestamp")) 0L else java.lang.Long.parseLong(lastRecord!!["timestamp"].toString())

    /**
     * Returns a type of collection of tasks, to which current task belongs (loggers, aggregators, archivers etc)
     * @return Collection name as string
     */
    override val collectionType: String
        get() = "loggers"

    init {
        this.syslog = Syslog(this)
    }

    internal constructor(name: String) : this() {
        this.name = name
    }

    internal constructor(config: HashMap<String, Any>) : this() {}

    internal constructor(name: String, downloader: IDownloader, parser: IParser) : this() {
        this.name = name
        this.downloader = downloader
        this.parser = parser
    }

    /**
     * Method used to apply configuration to data logger instance.
     * @param config: Configuration object
     */
    override fun configure(config: HashMap<String, Any>?) {
        super.configure(config)
        this.name = config!!["name"].toString()
        this.shouldWriteDuplicates = java.lang.Boolean.valueOf((config as java.util.Map<String, Any>).getOrDefault("shouldWriteDuplicates", "false").toString())
        this.destinationPath = (config as java.util.Map<String, Any>).getOrDefault("destinationPath", destinationPath).toString()
        this.mstatusPath = (config as java.util.Map<String, Any>).getOrDefault("statusPath", mstatusPath).toString()
        val fieldsToLog = (config as java.util.Map<String, Any>).getOrDefault("fieldsToLog", "").toString()
        if (!fieldsToLog.isEmpty())
            this.fieldsToLog = Arrays.asList(*fieldsToLog.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        if (this.syslog == null) this.syslog = Syslog(this)
        this.downloader.configure(config)
        this.parser.configure(config)
        this.propagateSyslog()
    }

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    override fun run() {
        super.run()
        log()
        taskStatus = CronjobTaskStatus.IDLE
        lastExecTime = Instant.now().epochSecond
    }

    /**
     * Matin method, used to read source data, transform and write to file
     */
    override fun log() {
        var record = readRecord()
        if (record != null)
            syslog!!.log(ISyslog.LogLevel.DEBUG, "Logger '" + this.name + "' received record '" + record.toString() + "'.",
                    this.javaClass.name, "log")
        if (!shouldWriteDuplicates) record = getChangedRecord(record)
        if (record == null) return
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Logger '" + this.name + "' filtered record '" + record.toString() + "'.",
                this.javaClass.name, "log")
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Logger '" + this.name + "' wrote record '" + record.toString() + "'.",
                this.javaClass.name, "log")
        writeRecord(record)
    }

    /**
     * Method used to read current data from source and parse it
     * @return: Object with extracted data fields and their values or null if could not extract data
     */
    fun readRecord(): HashMap<String, Any>? {
        val source = downloader.download()
        if (source.isEmpty()) {
            syslog!!.log(ISyslog.LogLevel.WARNING, "Downloader returned empty string",
                    this.javaClass.name, "readRecord")
            return null
        }
        parser.inputString = source
        val result = getFilteredRecord(parser.parse() as HashMap<String, Any>)
        if (result!!.isEmpty()) {
            syslog!!.log(ISyslog.LogLevel.WARNING, "Empty record returned after parsing",
                    this.javaClass.name, "readRecord")
            return null
        }
        (result as java.util.Map<String, Any>).putIfAbsent("timestamp", Instant.now().epochSecond.toString())
        return result
    }

    fun getFilteredRecord(record: HashMap<String, Any>?): HashMap<String, Any>? {
        return if (record == null || fieldsToLog == null || fieldsToLog!!.isEmpty()) record else record.keys.stream()
                .filter { key -> fieldsToLog!!.contains(key) || key == "timestamp" }
                .collect(Collectors.toMap(
                        { key -> key },
                        { record[it] },
                        { item1, item2 -> item1 },
                        { HashMap<String,Any>() }))
    }

    /**
     * Method used to compare provided data record with previous saved record
     * @param record Record to compare
     * @return True if provided record is different or false otherwise
     */
    internal fun isRecordChanged(record: HashMap<String, Any>?): Boolean {
        if (lastRecord == null) readAndSetLastRecord()
        if (lastRecord == null) return true
        if (record === lastRecord) return false
        if (record == null) return true
        if (record.keys.size != lastRecord!!.keys.size) return true
        for (key in record.keys) {
            if (key == "timestamp") continue
            if (!lastRecord!!.containsKey(key)) return true
            if (lastRecord!![key] != record[key]) return true
        }
        return false
    }

    /**
     * Method compares provided record with last processed record and returns new record
     * with only fields that changed
     * @param record Source record
     * @return Resulting record with changed fields
     */
    private fun getChangedRecord(record: HashMap<String, Any>?): HashMap<String, Any>? {
        if (record == null) return null
        if (lastRecord == null) readAndSetLastRecord()
        if (lastRecord == null) {
            lastRecord = record.clone() as HashMap<String, Any>
            return record
        }
        val resultRecord = HashMap<String, Any>()
        for (key in record.keys) {
            if (key === "timestamp") continue
            if (!lastRecord!!.containsKey(key) || lastRecord!![key] != record[key])
                resultRecord[key] = record[key]!!
        }
        if (resultRecord.size == 0) return null
        resultRecord["timestamp"] = record["timestamp"]!!
        lastRecord = record.clone() as HashMap<String, Any>
        return resultRecord
    }

    /**
     * Method used to get string value of last record from status file, parse it and setup
     */
    protected fun readAndSetLastRecord() {
        val record = readLastRecord()
        if (record == null || record.isEmpty()) return
        val gson = Gson()
        lastRecord = gson.fromJson(record, HashMap::class.java) as HashMap<String,Any>
    }

    /**
     * Method used to write provided record to file in JSON format
     * @param record: Input record
     */
    internal fun writeRecord(record: HashMap<String, Any>?) {
        if (record == null) return
        val recordPath = getRecordPath(record)
        val path = Paths.get(recordPath)
        try {
            if (!Files.exists(path.parent)) Files.createDirectories(path.parent)
            val json = getJson(record)
            val recordFile = Files.newBufferedWriter(path)
            recordFile.write(json)
            recordFile.flush()
            recordFile.close()
            writeLastRecord()
        } catch (e: IOException) {
            e.printStackTrace()
            syslog!!.logException(e, this, "writeRecord")
        }

    }

    /**
     * Method used to return Path to file, to which provided record will be written
     * @param record: Record to write
     * @return Full path to file in file system
     */
    internal fun getRecordPath(record: HashMap<String, Any>?): String {
        if (record == null) return ""
        val timestampStr = record["timestamp"].toString()
        val timestamp = timestampStr.toString().toLong()
        val date = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC)
        return getDestinationPath() + "/" + date.year + "/" + date.monthValue + "/" +
                date.dayOfMonth + "/" + date.hour + "/" + date.minute + "/" + date.second + ".json"
    }

    /**
     * Method used to convert data record to JSON string
     * @param record: Source record
     * @return JSON string of record
     */
    internal fun getJson(record: HashMap<String, Any>?): String {
        val gson = Gson()
        return if (record != null) gson.toJson(record) else ""
    }

    /**
     * Method returns destination path, which is a base path, to which logger writes data
     * @return String with path
     */
    fun getDestinationPath(): String {
        var resultPath: String? = destinationPath
        if (resultPath == null || resultPath.isEmpty())
            resultPath = LoggerApplication.instance.getCachePath() + "/loggers/" + this.name
        if (!Paths.get(resultPath).isAbsolute)
            resultPath = LoggerApplication.instance.getCachePath() + "/loggers/" + this.name + resultPath
        return resultPath
    }

    /**
     * Method used to set Syslog instance to Downloader and Parser instances of this logger
     */
    override fun propagateSyslog() {
        this.downloader.syslog = this.syslog!!
        this.parser.syslog = this.syslog!!
    }

    companion object {

        /**
         * Factory method, used to get instanse of logger of specified type
         * @param config: Configuration object, to configure logger before start
         * @return Instance of Data Logger class
         */
        fun create(config: HashMap<String, Any>): ILogger? {
            val loggerType = (config as java.util.Map<String, Any>).getOrDefault("className", "").toString()
            var result: ILogger? = null
            when (loggerType) {
                "YandexWeatherLogger" -> result = YandexWeatherLogger(config)
            }
            result?.propagateSyslog()
            return result
        }
    }
}