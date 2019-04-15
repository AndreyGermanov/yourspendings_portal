package readers

import com.google.gson.Gson
import main.ISyslog

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.stream.Collectors

/**
 * Class provide functions to get information from downloaded data folder.
 */
class FileDataReader
/**
 * Class constructor
 * @param filePath Full path to root folder
 */
(// Path of root directory
        private val filePath: String, // Instance of internal error logger used to log exceptions and other issues to file
        private val syslog: ISyslog) : IDataReader {
    // Cached list of file paths. Key of map is 'timestamp', value is full file path
    private var filesList: NavigableMap<Long, Path> = TreeMap()

    /**
     * Returns Time range of data. Includes first date and last date
     * @return Range object with timestamp of first record and timestamp of last record
     */
    override val range: DataRange
        get() {
            val source = getFilesList()
            return if (source.size == 0) DataRange() else DataRange(source.firstKey(), source.lastKey())
        }

    /**
     * Method fills "filesList" with all valid files inside folder
     * @return HashMap of files, ordered by timestamp
     */
    internal fun getFilesList(): NavigableMap<Long, Path> {
        return getFilesList(false)
    }

    /**
     * Method fills "filesList" with data files inside folder which are inside specified date range
     * @param startDate Timestamp of start date
     * @param endDate Timestamp of end date
     * @return HashMap of files, ordered by timestamp
     */
    internal fun getFilesList(startDate: Long?, endDate: Long?): NavigableMap<Long, Path> {
        var result: NavigableMap<Long, Path> = TreeMap()
        if (startDate!! > endDate!!) return result
        val range = getRangeBounds(startDate, endDate)
        if (range.startDate.equals(0) || range.endDate.equals(0)) return result
        try {
            result = getFilesList().subMap(range.startDate, true, range.endDate, true)
        } catch (e: Exception) {
        }

        return result
    }

    /**
     * Base method to fill "filesList" with all valid files inside folder
     * @param refreshCache Should this method reread files list from filesystem or just return cached one
     * @return HashMap of files, ordered by timestamp
     */
    private fun getFilesList(refreshCache: Boolean): NavigableMap<Long, Path> {
        var result = filesList
        val path = Paths.get(filePath)
        if (!Files.exists(path)) return result
        try {
            if (refreshCache || filesList.size == 0) {
                result = Files.walk(path)
                        .filter { p -> Files.isRegularFile(p) && p.toString().endsWith(".json") }
                        .collect(Collectors.toMap({ this.getPathTimestamp(it) },
                                { p -> p },
                                { v1, v2 ->
                                    throw RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2))
                                }, { TreeMap<Long,Path>() }))
                filesList = result
            }
        } catch (e: IOException) {
            syslog.logException(e, this, "getFilesList")
        }

        return result
    }

    /**
     * Method construct timestamp of data file using it full path (if path correctly formatted)
     * @param path: Source path
     * @return Generated timestamp
     */
    private fun getPathTimestamp(path: Path): Long {
        val parts = path.toString().split("/".toRegex()).dropLastWhile { it.isEmpty() }
        val second = parts[parts.size - 1].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.get(0).toInt()
        val minute = parts[parts.size - 2].toInt()
        val hour = parts[parts.size - 3].toInt()
        val day = parts[parts.size - 4].toInt()
        val month = parts[parts.size - 5].toInt()
        val year = parts[parts.size - 6].toInt()
        return LocalDateTime.of(year, month, day, hour, minute, second).toEpochSecond(ZoneOffset.UTC)
    }

    /**
     * Method returns closest date range of data, based on provided date range
     * @param startDate Timestamp of start
     * @param endDate Timestamp of end
     * @return Calculated date range
     */
    private fun getRangeBounds(startDate: Long?, endDate: Long?): DataRange {
        return DataRange(getStartRangeBound(startDate)!!, getEndRangeBound(endDate)!!)
    }

    /**
     * Method finds timestamp of record, which is closest to specified date (same date or later)
     * @param value Input timestamp
     * @return Closest timestamp of data (can be the same timestamp or later closest one)
     */
    private fun getStartRangeBound(value: Long?): Long? {
        return getRangeBound(value, true)
    }

    /**
     * Method finds timestamp of record, which is closest to specified date (same date or earlier)
     * @param value Input timestamp
     * @return Closest timestamp of data (can be the same timestamp or earlier closest one)
     */
    private fun getEndRangeBound(value: Long?): Long? {
        return getRangeBound(value, false)
    }

    /**
     * Base method which returns timestamp of record which is closest to provided one
     * @param value Input timestamp
     * @param findHigher : Direction: if true, than will return later closest timestamp, otherwise earlier closest timestamp
     * @return Closest timestamp of data based on provided options
     */
    private fun getRangeBound(value: Long?, findHigher: Boolean): Long? {
        val value:Long = value ?: 0L
        val source = getFilesList()
        if (source.size == 0) return 0L
        if (source.containsKey(value)) return value
        var result: Long? = if (findHigher) if (value < source.firstKey()) source.firstKey() else value else if (value > source.lastKey()) source.lastKey() else value
        val closeKey = if (findHigher) source.higherKey(value) else source.lowerKey(value)
        if (closeKey != null) result = closeKey
        return result
    }

    /**
     * Returns statistical information about data: Date range and number of records
     * @return DataStats object with start timestamp, end timestamp and number of records
     */
    override fun getDataStats(refreshCache: Boolean): DataStats {
        return getDataStats(0L, 0L, refreshCache)
    }

    /**
     * Returns statistical information about data inside requested date range, it includes timestamp of first
     * record, timestamp of last record and number of records in this period.
     * @param startDate Start date
     * @param endDate End date
     * @return DataStats object with start timestamp, end timestamp and number of records
     */
    override fun getDataStats(startDate: Long?, endDate: Long?, refreshCache: Boolean): DataStats {
        if (refreshCache) getFilesList(refreshCache)
        val range = getRangeBounds(startDate, endDate)
        return DataStats(range, getFilesList(startDate, endDate).size)
    }

    /**
     * Method read data from all files and returns it as a HashMap, ordered by timestamp
     * @return HashMap with timestamp as key and data record (HashMap<String></String>,Object>) as value
     */
    override fun getData(refreshCache: Boolean): NavigableMap<Long, HashMap<String, Any>> {
        val range = range
        return getData(range.startDate, range.endDate, refreshCache)
    }

    /**
     * Method read data from files inside specified date range and returns it as a HashMap, ordered by timestamp
     * @param startDate Start timestamp
     * @param endDate End timestamp
     * @return HashMap with timestamp as key and data record (HashMap<String,Object>) as value
     */
    @Override
    override fun getData(startDate:Long?, endDate:Long?, refreshCache:Boolean):NavigableMap<Long,HashMap<String,Any>> {
        val result = TreeMap<Long,HashMap<String,Any>>()
        val stats = getDataStats(startDate,endDate,refreshCache);
        if (stats.count==0) return result
        getFilesList(startDate,endDate).entries.parallelStream().forEach{ entry ->
            val record = getDataRecord(entry.value)
            if (record != null) {
                val timestamp = record["timestamp"].toString().toLong()
                synchronized(this) {
                    result.put(timestamp, record)
                }
            }
        }
        return result
    }

    /**
     * Method read data from files inside specified date range and returns it as a HashMap, ordered by timestamp
     * @param startDate Start timestamp
     * @return HashMap with timestamp as key and data record (HashMap<String></String>,Object>) as value
     */
    override fun getData(startDate: Long?, refreshCache: Boolean): NavigableMap<Long, HashMap<String, Any>> {
        return getData(startDate, Instant.now().epochSecond, refreshCache)
    }

    /**
     * Method used to read single record from data file
     * @param path Path to datafile
     * @return record as HashMap<String></String>,Object> or null in case of errors
     */
    private fun getDataRecord(path: Path): HashMap<String, Any>? {
        val gson = Gson()
        try {
            Files.newBufferedReader(path).use { reader ->
                if (!Files.exists(path) || Files.size(path) == 0L) return null
                val content = reader.readLine()
                if (content.isEmpty()) return null
                val record = gson.fromJson(content, HashMap::class.java) as? HashMap<String,Any> ?: HashMap()
                return if (record.size == 0 || !record.containsKey("timestamp")) null else record
            }
        } catch (e: Exception) {
            syslog.logException(e, this, "getDataRecord")
            return null
        }

    }

    /**
     * Class which holds information about date range
     */
    class DataRange {
        var startDate: Long = 0L
        var endDate: Long = 0L

        internal constructor(startDate: Long, endDate: Long) {
            this.startDate = startDate
            this.endDate = endDate
        }

        internal constructor() {}
    }

    /**
     * Class which holds statistical information about data folder
     */
    inner class DataStats internal constructor(var range: DataRange, var count: Int)
}