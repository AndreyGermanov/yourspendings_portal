package readers

import java.util.HashMap
import java.util.NavigableMap

/**
 * Interface which all data readers must implement
 */
interface IDataReader {
    val range: FileDataReader.DataRange
    fun getDataStats(refreshCache: Boolean): FileDataReader.DataStats
    fun getDataStats(startDate: Long?, endDate: Long?, refreshCache: Boolean): FileDataReader.DataStats
    fun getData(refreshCache: Boolean): NavigableMap<Long, HashMap<String, Any>>
    fun getData(startDate: Long?, refreshCache: Boolean): NavigableMap<Long, HashMap<String, Any>>
    fun getData(startDate: Long?, endDate: Long?, refreshCache: Boolean): NavigableMap<Long, HashMap<String, Any>>

    companion object {
        fun getDataRange(startDate: Long?, endDate: Long?): FileDataReader.DataRange {
            return FileDataReader.DataRange(startDate!!, endDate!!)
        }
    }
}
