package archivers

import java.nio.file.Path
import java.util.HashMap

/**
 * Zip archiver specific to archive data folder structures, captured by loggers
 * and aggregated by aggregators
 */
class DataZipDataArchiver
/**
 * Class constructor
 *
 * @param config - Configuration object
 */
internal constructor(config: HashMap<String, Any>) : FileZipDataArchiver(config) {

    /**
     * Method used as a filter to determine, should provided file be archived or not
     * @param file Path to file to check
     * @return True if file should be archived or false otherwise
     */
    override fun checkFile(file: Path): Boolean {
        return if (!file.toString().endsWith(".json")) false else super.checkFile(file)
    }

    /**
     * Method used to get timestmap of file
     * @param file - File to get timestamp of
     * @return Timestamp of file
     */
    override fun getFileTimestamp(file: Path): Long {
        return getFileTimestamp(file, true) ?: 0L
    }
}