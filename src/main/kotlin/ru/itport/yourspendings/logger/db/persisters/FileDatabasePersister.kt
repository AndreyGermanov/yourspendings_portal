package db.persisters

import com.google.gson.Gson
import config.ConfigManager
import db.adapters.DatabaseAdapter
import db.adapters.IDatabaseAdapter
import main.ISyslog
import main.Syslog
import readers.IDataReader
import readers.FileDataReader

import java.util.*
import java.util.stream.Collectors

class FileDatabasePersister : DatabasePersister, ISyslog.Loggable {

    // Unique name of persister
    override var name = ""
    // Link to adapter, which provides database access settings
    private var databaseAdapter: IDatabaseAdapter? = null
    // Path to folder with aggregated source data
    private var sourcePath = ""
    // Name of destination collection (table) in database
    private var collectionName = ""
    // Should this persister write duplicate rows
    private var writeDuplicates = false
    // Should this persister fill gaps in data using values from previous rows
    private var fillDataGaps = false
    // Link to data reader instance, which used to manage data reading process from source folder
    private var sourceDataReader: IDataReader? = null
    // How many rows should this persister write to database per single run. If 0, then will process all data in
    // source folder
    private var rowsPerRun = 0
    // Path to folder, in which this persister write temporary status information, like last processed row
    override var mstatusPath = ""
    // Last processed record
    var mlastRecord: HashMap<String, Any>? = null
    override var lastRecord:HashMap<String, Any>?
        get() = mlastRecord
        set(value) {mlastRecord = value?.clone() as? HashMap<String,Any>}
    /**
     * Returns serialized information about last record as a string, ready to write to file in "statusPath"
     * @return String representation of last record or null if not able to produce this string
     */
    override val lastRecordString: String?
        get() {
            if (mlastRecord == null) return null
            val gson = Gson()
            return gson.toJson(mlastRecord)
        }

    override val lastRecordTimestamp: Long
        get() {
            return if (lastRecord == null || !mlastRecord!!.containsKey("timestamp")) 0L else java.lang.Long.parseLong(mlastRecord!!.get("timestamp").toString())
        }

    /**
     * Class constructor
     * @param config - Configuration object
     */
    constructor(config: HashMap<String, Any>) {
        this.configure(config)
    }

    /**
     * Class constructor
     * @param name - Name of persister
     * @param sourcePath - Path to source data folder
     * @param databaseAdapter - Name of database adapter in configuration
     */
    internal constructor(name: String, sourcePath: String, databaseAdapter: String) {
        this.configure(hashMapOf("name" to name, "sourcePath" to sourcePath, "databaseAdapter" to databaseAdapter))
    }

    /**
     * Class constructor
     * @param name - Name of persister
     */
    internal constructor(name: String) {
        this.configure(ConfigManager.getInstance().getDatabasePersister(name))
    }

    /**
     * Method used to load settings of this persister from configuration object, provided by configuration manager
     * from configuration file
     * @param config - Configuration object
     */
    override fun configure(config: HashMap<String, Any>?) {
        super.configure(config)
        name = (config as java.util.Map<String, Any>).getOrDefault("name", name).toString()
        sourcePath = (config as java.util.Map<String, Any>).getOrDefault("sourcePath", sourcePath).toString()
        collectionName = (config as java.util.Map<String, Any>).getOrDefault("collectionName", collectionName).toString()
        writeDuplicates = java.lang.Boolean.parseBoolean((config as java.util.Map<String, Any>).getOrDefault("writeDuplicates", writeDuplicates).toString())
        fillDataGaps = java.lang.Boolean.parseBoolean((config as java.util.Map<String, Any>).getOrDefault("fillDataGaps", fillDataGaps).toString())
        rowsPerRun = java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("rowsPerRun", 0).toString()).toInt()
        mstatusPath = (config as java.util.Map<String, Any>).getOrDefault("statusPath", mstatusPath).toString()
        if (config!!.containsKey("databaseAdapter")) databaseAdapter = DatabaseAdapter[config!!["databaseAdapter"].toString()]
        if (syslog == null) syslog = Syslog(this)
        sourceDataReader = FileDataReader(sourcePath, syslog as Syslog)
    }

    /**
     * Entry point method. Used to start process of writing source data to database
     * @return
     */
    override fun persist(): Int? {
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Data persister '" + this.name + "' started to persist...",
                this.javaClass.name, "persist")
        mlastRecord = HashMap()
        val data = prepareData()
        if (data == null || data!!.size == 0) return null
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Data persister '" + this.name + "' got data record " + data!!.toString(),
                this.javaClass.name, "persist")
        val insertedRowsCount = databaseAdapter!!.insert(collectionName, data!!)
        if (insertedRowsCount == null || insertedRowsCount == 0) {
            syslog!!.log(ISyslog.LogLevel.ERROR, "Data persister '" + this.name + "' could not write data record " + data!!.toString(),
                    this.javaClass.name, "persist")
            return null
        }
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Data persister '" + this.name + "' wrote data record " + data!!.toString(),
                this.javaClass.name, "persist")
        writeLastRecord()
        return insertedRowsCount
    }

    /**
     * Method used to read source data and transform it to format, ready for data adapter
     * to write to database
     * @return
     */
    private fun prepareData(): ArrayList<HashMap<String, Any>>? {
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Data persister '" + this.name + "'. Begin prepare data to persist",
                this.javaClass.name, "prepareData")
        if (sourceDataReader == null) return null
        readAndSetLastRecord()
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Data persister '" + this.name + "'. Read last data record.",
                this.javaClass.name, "prepareData")
        var startDate: Long = 0L
        if (mlastRecord != null) startDate = java.lang.Long.parseLong(mlastRecord!!["timestamp"].toString())
        if (startDate > 0) startDate += 1
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Data persister '" + this.name + "'. Last record timestamp = ." + startDate,
                this.javaClass.name, "prepareData")
        val data = sourceDataReader!!.getData(startDate, true)
        syslog!!.log(ISyslog.LogLevel.DEBUG, "Data persister '" + this.name + "'. Got data ." + data,
                this.javaClass.name, "prepareData")
        return if (data == null || data!!.size == 0)
            null
        else
            data!!.values.stream()
                    .sorted(Comparator.comparingInt<HashMap<String, Any>> { s -> Integer.parseInt(s.get("timestamp").toString()) })
                    .filter({ record -> !isDuplicateRecord(record) })
                    .limit((if (rowsPerRun > 0) rowsPerRun else data!!.size).toLong())
                    .peek { lastRecord = it }
                    .collect(Collectors.toList()) as ArrayList<HashMap<String,Any>>
    }

    /**
     * Method used to check if provided record contains the same data as last processed record
     * @param record Record to check
     * @return True if records are equal or false otherwise
     */
    private fun isDuplicateRecord(record: HashMap<String, Any>?): Boolean {
        if (record == null || lastRecord == null) return false
        if (record!!.size != lastRecord!!.size) return false
        for (key in record!!.keys) {
            if (!lastRecord!!.containsKey(key)) return false
            if (key == "timestamp") continue
            if (record!![key].toString() != lastRecord!![key].toString()) return false
        }
        syslog!!.log(ISyslog.LogLevel.DEBUG, "NOT DUPLICATED", this.javaClass.name, "isDuplicateRecord")
        return true
    }

    /**
     * Method used to get string value of last record from status file, parse it and setup
     */
    fun readAndSetLastRecord() {
        val result = readLastRecord()
        if (result == null) {
            mlastRecord = null
            return
        }
        val gson = Gson()
        lastRecord = gson.fromJson(result, HashMap::class.java) as HashMap<String,Any>
    }

}