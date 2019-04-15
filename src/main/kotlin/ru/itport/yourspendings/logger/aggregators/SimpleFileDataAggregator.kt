package aggregators;

import com.google.gson.Gson
import config.ConfigManager
import main.ISyslog
import main.LoggerApplication
import main.Syslog
import net.objecthunter.exp4j.ExpressionBuilder
import readers.FileDataReader
import readers.IDataReader
import utils.MathUtils
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.stream.Stream

/**
 * Class used to aggregate data, collected by logger in folder of filesystem.
 */
class SimpleFileDataAggregator : DataAggregator, ISyslog.Loggable {

    /// Field definitions. Contains information about data fields, which should be aggregated
    internal var fieldDefs = HashMap<String, HashMap<String, Any>>()

    // Aggregation period in seconds
    private var aggregationPeriod = 5

    // How much aggregation periods of source data to process per single run. If 0, then unlimited e.g. all data
    private var aggregatesPerRun = 0

    // Should gaps in data be filled with values of previous period (or from next period if no previous period)
    private var fillDataGaps = true

    /// Should write duplicate data (if current record is the same as previous).
    var shouldWriteDuplicates = false

    // Full path to the folder with source data
    private var sourcePath = ""

    // Link to FileDataReader object, which will be used to work with source data files
    private var sourceDataReader: IDataReader? = null

    var aggregatorDataReader: IDataReader? = null

    // Unique name of this aggregator
    override var name = ""

    // Full path to destination folder to which destination aggregated data will be written. If empty, then full path
    // will be automatically calculated based on application cache path
    private var destinationPath = ""

    override var lastRecord: HashMap<String, Any>? = null

    /**
     * Method calculates time interval to aggregate data
     * @return Range with startDate and endDate
     */
    internal val aggregationRange: FileDataReader.DataRange
        get() {
            if (lastRecord == null || !lastRecord!!.containsKey("timestamp")) readAndSetLastRecord()
            syslog!!.log(ISyslog.LogLevel.DEBUG, "Last record " + lastRecord!!.toString(), this.javaClass.name, "getAggregationRange")
            var startDate: Long? = java.lang.Long.parseLong((lastRecord as java.util.Map<String, Any>).getOrDefault("timestamp", 0).toString())
            var endDate: Long? = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            if (this.aggregatesPerRun != 0) endDate = startDate!! + aggregatesPerRun * aggregationPeriod
            val sourceDataRange = sourceDataReader!!.getDataStats(startDate, endDate, true)
            startDate = alignDate(java.lang.Long.max(startDate!!, sourceDataRange.range.startDate))
            endDate = alignDate(java.lang.Long.min(endDate!!, sourceDataRange.range.endDate))
            return IDataReader.getDataRange(startDate, endDate)
        }

    public override var syslog: ISyslog?
        get() {
            if (this.syslog == null)
                this.syslog = Syslog(this)
            return this.syslog
        }
        set(syslog) {
            this.syslog = syslog
        }

    /**
     * Returns root directory, in which currect aggregator writes it's data
     * @return Full path to file in filesystem
     */
    internal val aggregatorPath: String
        get() {
            var resultPath = destinationPath
            if (resultPath.isEmpty())
                resultPath = LoggerApplication.instance.getCachePath() + "/" + collectionType + "/" + this.name
            if (!Paths.get(resultPath).isAbsolute)
                resultPath = LoggerApplication.instance.getCachePath() + "/" + resultPath
            return resultPath
        }

    /**
     * Returns serialized information about last record as a string, ready to write to file in "statusPath"
     * @return String representation of last record or null if not able to produce this string
     */
    override val lastRecordString: String?
        get() = if (lastRecord == null) null else Gson().toJson(lastRecord)

    override val lastRecordTimestamp: Long
        get() =
            if (lastRecord == null || !lastRecord!!.containsKey("timestamp")) 0L else java.lang.Long.parseLong(lastRecord!!["timestamp"].toString())

    /**
     * Class constructors
     */
    constructor(name: String, sourcePath: String) {
        val config = HashMap<String, Any>()
        config["name"] = name
        config["sourcePath"] = sourcePath
        config["destinationPath"] = destinationPath
        this.configure(config)
    }

    constructor(config: HashMap<String, Any>) {
        this.configure(config)
    }

    constructor(name: String) {
        this.configure(ConfigManager.getInstance().getDataAggregator(name))
    }

    /**
     * Method used to apply configuration to object.
     * @param config: Configuration object
     */
    override fun configure(config: HashMap<String, Any>?) {
        super.configure(config)
        this.name = (config as java.util.Map<String, Any>).getOrDefault("name", this.name).toString()
        this.sourcePath = (config as java.util.Map<String, Any>).getOrDefault("sourcePath", this.sourcePath).toString()
        this.destinationPath = (config as java.util.Map<String, Any>).getOrDefault("destinationPath", this.destinationPath).toString()
        this.fieldDefs = (config as java.util.Map<String, Any>).getOrDefault("fields", this.fieldDefs) as HashMap<String, HashMap<String, Any>>
        this.fillDataGaps = (config as java.util.Map<String, Any>).getOrDefault("fillDataGaps", this.fillDataGaps) as Boolean
        this.aggregationPeriod = java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("aggregationPeriod", this.aggregationPeriod).toString()).toInt()
        this.aggregatesPerRun = java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("aggregatesPerRun", this.aggregatesPerRun).toString()).toInt()
        this.syslog = this.syslog
        this.sourceDataReader = FileDataReader(this.sourcePath, this.syslog!!)
    }

    /**
     * Main entry point. Method loads source data from files, applies aggregation rules to data fields using configuration
     * and stores aggregated data to destination folder
     */
    override fun aggregate() {
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Aggregator '" + this.name + "' started ...", this.javaClass.name, "aggregate")
        val range = aggregationRange
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Aggregator '" + this.name + "' received aggregation range ..." + range.startDate + "-" + range.endDate,
                this.javaClass.name, "aggregate")
        Stream.iterate(range.startDate) { timestamp: Long -> timestamp + aggregationPeriod }
                .limit(Math.round(((range.endDate - range.startDate) / aggregationPeriod).toFloat()).toLong())
                .forEach({ this.aggregateInterval(it) })
    }

    /**
     * Method used to round timestamp to closest aggregation interval
     * @param timestamp: Timestamp to align
     * @return Aligned timestamp
     */
    internal fun alignDate(timestamp: Long?): Long? {
        return Math.floor((timestamp!! / aggregationPeriod).toDouble()).toLong() * aggregationPeriod
    }

    /**
     * Method used to write single aggregation interval
     * @param startDate: Start date of interval
     */
    internal fun aggregateInterval(startDate: Long?) {
        val data = sourceDataReader!!.getData(startDate!! + 1,
                startDate + aggregationPeriod, false)
        val stats = getAggregateStats(data)
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Aggregator '" + this.name + "' started interval " + startDate + "-" + (startDate + aggregationPeriod),
                this.javaClass.name, "aggregate")
        if (stats.size == 0) return
        var aggregate = HashMap<String, Any>()
        for (fieldName in stats.keys) {
            val value = getAggregatedValue(fieldName, stats[fieldName])
            if (value != null) aggregate[fieldName] = value
        }
        if (aggregate.size == 0) return
        aggregate = markRecord(startDate, aggregate)
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Aggregator '" + this.name + "' created record " + aggregate.toString(),
                this.javaClass.name, "aggregate")
        writeRecord(aggregate)
    }

    /**
     * Method used to write aggregate record to file
     * @param aggregate Record to write
     */
    private fun writeRecord(aggregate: HashMap<String, Any>) {
        val path = Paths.get(getRecordPath(aggregate))
        try {
            if (!Files.exists(path.parent)) Files.createDirectories(path.parent)
            if (Files.exists(path)) Files.delete(path)
            val writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE_NEW)
            writer.write(Gson().toJson(aggregate))
            writer.flush()
            writer.close()
            lastRecord = aggregate.clone() as HashMap<String, Any>
            writeLastRecord()
            syslog!!.log(ISyslog.LogLevel.DEBUG, "Aggregator '" + this.name + "' wrote record to" + path.toString(),
                    this.javaClass.name, "aggregate")
        } catch (e: Exception) {
            syslog!!.logException(e, this, "aggregateInterval")
        }

    }

    /**
     * Method used to calculate aggregated value from data, collected in interval
     * @param fieldName: Name of field, for which calculate aggregated value
     * @param stats: Summarized interval data
     * @return Calculated value or null in case of errors
     */
    internal fun getAggregatedValue(fieldName: String, stats: AggregateFieldStats?): Any? {
        if (stats == null) return null
        val fieldConf = this.fieldDefs[fieldName] ?: return null
        val precision = (fieldConf as java.util.Map<String, Any>).getOrDefault("precision", 2) as Int
        var result: Any? = null
        when ((fieldConf as java.util.Map<String, Any>).getOrDefault("aggregate_function", "constant").toString()) {
            "count" -> result = stats.count
            "sum" -> result = stats.sum
            "min" -> result = stats.min
            "max" -> result = stats.max
            "first" -> result = stats.first
            "last" -> result = stats.last
            "constant" -> result = stats.first
            "average" -> {
                if (stats.count == 0) return null
                result = stats.sum!! / stats.count!!
            }
        }
        return if (result is Double) MathUtils.round(result, precision) else result
    }

    /**
     * Method returns summarized data for each field from provided source data array
     * @param data: Source data array
     * @return HashMap of statistical objects for each field.
     */
    internal fun getAggregateStats(data: NavigableMap<Long, HashMap<String, Any>>): HashMap<String, AggregateFieldStats> {
        val result = HashMap<String, AggregateFieldStats>()
        for (timestamp in data.keys) {
            val record = data[timestamp] ?: HashMap()
            if (record.size == 0) continue
            for (fieldName in this.fieldDefs.keys) {
                var stats: AggregateFieldStats? = result[fieldName]
                val value = calculateFieldValue(fieldName, record)
                stats = addEntryToFieldStats(value, stats)
                if (stats != null) result[fieldName] = stats
            }
        }
        return result
    }

    /**
     * Calculate value of field for aggregation.
     * @param fieldName: Name of field
     * @param record: Record of source data
     * @return Calculated value or null in case of errors
     */
    internal fun calculateFieldValue(fieldName: String, record: HashMap<String, Any>): Any? {
        val fieldConf = this.fieldDefs[fieldName] ?: HashMap()
        return if (fieldConf.containsKey("expression") && !fieldConf["expression"].toString().isEmpty())
            evaluateExpression(fieldConf["expression"].toString(), record)
        else if (fieldConf.containsKey("field") && !fieldConf["field"].toString().isEmpty())
            record[fieldName]
        else if (fieldConf.containsKey("constant") && !fieldConf["constant"].toString().isEmpty())
            evaluateConstant(fieldConf["constant"].toString())
        else
            null
    }

    /**
     * Calculate value of field, if formula expression specified for this field config
     * @param expression: Formula
     * @param record: Source data record
     * @return: Calculated value or null in case of errors
     */
    internal fun evaluateExpression(expression: String, record: HashMap<String, Any>): Double? {
        try {
            val calculator = ExpressionBuilder(expression).variables(record.keys).build()
            for (fieldName in record.keys) {
                val rawValue = record[fieldName]
                try {
                    val value = java.lang.Double.valueOf(rawValue.toString())
                    calculator.setVariable(fieldName, value)
                } catch (e: NumberFormatException) {
                }

            }
            return calculator.evaluate()
        } catch (e: Exception) {
            //syslog.log(Syslog.LogLevel.WARNING,"Could not process expression '"+expression+"' for record '"+
            //record+"'. Exception thrown: "+e.getMessage()+".",this.getClass().getName(),"evaluateExpression");
            return null
        }

    }

    /**
     * Method evaluates value of type "constant" for field
     * @param constantValue
     * @return
     */
    internal fun evaluateConstant(constantValue: String): Any {
        when (constantValue) {
            "\$aggregatorId" -> return this.name
            "\$aggregationPeriod" -> return this.aggregationPeriod
            else -> return constantValue
        }
    }

    /**
     * Adds value of field to summarized statistical object
     * @param value: Value to add
     * @param stats Source statistical object
     * @return Statistical object with added value to it fields
     */
    internal fun addEntryToFieldStats(value: Any?, stats: AggregateFieldStats?): AggregateFieldStats? {
        var stats = stats
        if (value == null) return stats
        if (stats == null) stats = AggregateFieldStats()
        if (stats.first == null) stats.first = value
        stats.last = value
        try {
            val decimalValue = java.lang.Double.valueOf(value.toString())
            stats.init()
            stats.sum += decimalValue
            stats.count += 1
            if (decimalValue > stats.max) stats.max = decimalValue
            if (decimalValue < stats.min) stats.min = decimalValue
        } catch (e: Exception) {
        }

        return stats
    }

    /**
     * Method used to add current aggregator identification fields to data record before writing it to
     * destinaton folder
     * @param timestamp timestamp of record
     * @param record record to mark
     * @return Marked record
     */
    internal fun markRecord(timestamp: Long?, record: HashMap<String, Any>): HashMap<String, Any> {
        record["timestamp"] = timestamp!!.toString()
        return record
    }

    fun setSourceDataReader(sourceDataReader: IDataReader) {
        this.sourceDataReader = sourceDataReader
    }

    /**
     * Method used to return Path to file, to which provided record will be written
     * @param record: Record to write
     * @return Full path to file in file system
     */
    internal fun getRecordPath(record: HashMap<String, Any>?): String {
        if (record == null) return ""
        val basePath = this.aggregatorPath
        val timestampStr = record["timestamp"].toString()
        val timestamp = timestampStr.toString().toLongOrNull() ?: 0L
        val date = LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC)
        return basePath + "/" + date.year + "/" + date.monthValue + "/" +
                date.dayOfMonth + "/" + date.hour + "/" + date.minute + "/" + date.second + ".json"
    }

    /**
     * Method used to get string value of last record from status file, parse it and setup
     */
    protected fun readAndSetLastRecord() {
        val record = readLastRecord()
        lastRecord = HashMap()
        if (record == null || record.isEmpty()) return
        val gson = Gson()
        lastRecord = gson.fromJson(record, HashMap::class.java) as HashMap<String,Any>
    }

    /**
     * Class, which holds summarized information of single field in aggregated interval
     */
    inner class AggregateFieldStats {
        internal var max: Double = 0.0
        internal var min: Double = 0.0
        internal var first: Any? = null
        internal var last: Any? = null
        internal var count: Int = 0
        internal var sum: Double = 0.0
        fun init() {
            if (sum == null) sum = 0.0
            if (count == null) count = 0
            if (max.equals(0)) max = -999999999.0
            if (min.equals(0)) min = 999999999.0
        }
    }
}