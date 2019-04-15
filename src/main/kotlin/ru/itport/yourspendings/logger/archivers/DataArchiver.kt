package archivers

import archivers.processors.ArchiveProcessor
import archivers.processors.IArchiveProcessor
import config.ConfigManager
import cronjobs.CronjobTask
import cronjobs.CronjobTaskStatus
import main.ISyslog
import main.LoggerApplication
import main.Syslog
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Arrays
import java.util.HashMap
import java.util.regex.Pattern

/**
 * Data Archiver base class. Used to create Data archiver components. Data archiver used to archive all files
 * in specified folder to destination folder in various formats or just by copy files
 */
abstract class DataArchiver : CronjobTask, IDataArchiver, ISyslog.Loggable {

    // Destination path in which archiver created
    override var mdestinationPath = ""
    // Source path of files to archive
    override var sourcePath = ""

    // Timestamp of last archive file, used to determine from which file to begin (to not archive older files)
    protected var lastFileTimestamp: Long = 0L
    // Last processed file name (to not process it twice)
    protected var lastFileName = ""
    // Unique name of current archive
    override var name = ""

    // Maximum number of files to archive per single run (0 - unlimited)
    private var maxArchiveFilesCount = 0L
    // Maximum size of data to archive per single run (0 - unlimited)
    private var maxArchiveSize = 0L
    // Should archiver remove archived files from source folder after process
    override var removeSourceAfterArchive = false

    // Link to syslog daemon, which used to write errors and warningss
    override var syslog: ISyslog? = null

    // Link to file processor, which used to implement archive operation (depends on type of archiver: File, Zip, etc)
    protected var processor: IArchiveProcessor? = null
    // Caches information about number of processed archive files per current run
    override var archivedFilesCount = 0L
    // Caches information about summary size of data processed per current run
    var archivedFilesSize = 0L
    // Regular expression which used as a filter for filenames, that should be processed by archiver
    private var filterRegex = ""

    /**
     * Returns serialized information about last record as a string, ready to write to file in "statusPath"
     * @return String representation of last record or null if not able to produce this string
     */
    override val lastRecordString: String?
        get() =
            if (lastFileName.isEmpty() || lastFileTimestamp == 0L) null else lastFileTimestamp!!.toString() + " " + lastFileName

    override val lastRecordTimestamp: Long
        get() = if (lastFileTimestamp <= 0) 0L else lastFileTimestamp!!

    /**
     * Returns a type of collection of tasks, to which current task belongs (loggers, aggregators, archivers etc)
     * @return Collection name as string
     */
    override val collectionType: String
        get() = "archivers"

    override val lastRecord: String?
        get() = this.lastRecordString

    /**
     * Class constructor
     * @param name Unique name of archiver
     */
    internal constructor(name: String) {
        if (!name.isEmpty()) configure(ConfigManager.getInstance().getDataArchiver(name))
    }

    /**
     * Class constructor
     * @param config Configuration object to configure archiver
     */
    internal constructor(config: HashMap<String, Any>) {
        configure(config)
    }

    /**
     * Class constructor
     * @param name - Unique name of archiver
     * @param type - Type of archiver
     * @param sourcePath - Source path with data to archiver
     * @param destinationPath - Destination path in which create archives
     */
    internal constructor(name: String, type: String, sourcePath: String, destinationPath: String) {
        configure(hashMapOf("name" to name, "type" to type, "sourcePath" to sourcePath, "destinationPath" to destinationPath))
    }

    /**
     * Method used to set parameters of archiver from provied configuration object
     * @param config Configuration object
     */
    override fun configure(config: HashMap<String, Any>?) {
        super.configure(config)
        name = (config as java.util.Map<String, Any>).getOrDefault("name", name).toString()
        mstatusPath = (config as java.util.Map<String, Any>).getOrDefault("statusPath", mstatusPath).toString()
        mdestinationPath = (config as java.util.Map<String, Any>).getOrDefault("destinationPath", mdestinationPath).toString()
        sourcePath = (config as java.util.Map<String, Any>).getOrDefault("sourcePath", sourcePath).toString()
        maxArchiveSize = java.lang.Long.parseLong((config as java.util.Map<String, Any>).getOrDefault("maxArchiveSize", maxArchiveFilesCount).toString())
        maxArchiveFilesCount = java.lang.Long.parseLong((config as java.util.Map<String, Any>).getOrDefault("maxArchiveFilesCount", maxArchiveFilesCount).toString())
        removeSourceAfterArchive = java.lang.Boolean.parseBoolean((config as java.util.Map<String, Any>).getOrDefault("removeSourceAfterArchive", removeSourceAfterArchive).toString())
        filterRegex = (config as java.util.Map<String, Any>).getOrDefault("filterRegex", filterRegex).toString()
        if (syslog == null) syslog = Syslog(this)
        processor = ArchiveProcessor.create((config as java.util.Map<String, Any>).getOrDefault("type", "").toString(), this)
        if (processor != null) processor!!.configure(config)
    }

    /**
     * Main method to start archiving procedure
     * @return Number of archived files
     */
    override fun archive(): Long {
        var result = 0L
        archivedFilesCount = 0L
        archivedFilesSize = 0L
        if (processor == null || !processor!!.validateAndInitArchive()) return result
        readAndSetLastRecord()
        result = archiveFiles()
        processor!!.finish()
        writeLastRecord()
        return result
    }

    /**
     * Method used to get string value of last record from status file, parse it and setup
     */
    fun readAndSetLastRecord() {
        val record = readLastRecord()
        if (record == null || record.isEmpty()) return
        try {
            val tokens = record.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (tokens.size < 2) return
            lastFileTimestamp = java.lang.Long.parseLong(tokens[0])
            lastFileName = Arrays.stream(tokens).skip(1).reduce { s, s1 -> "$s $s1" }.orElse("")
        } catch (e: NumberFormatException) {
            syslog!!.log(ISyslog.LogLevel.ERROR, "Could not parse timestamp from last record '" + record + "'." +
                    "Error message+'" + e.message + "'", this.javaClass.name, "readAndSetLastRecord")
        }

    }

    /**
     * Method which used to get list of files to process, filter them and pass to archive processor
     * to archive
     * @return Number of processed files
     */
    private fun archiveFiles(): Long {
        try {
            Files.walk(Paths.get(sourcePath)).filter { p -> Files.isRegularFile(p) }
                    .sorted { file1, file2 -> this.sortFiles(file1, file2) }
                    .filter { this.checkFile(it) }
                    .forEach { this.processFile(it) }
            return archivedFilesCount
        } catch (e: IOException) {
            syslog!!.log(ISyslog.LogLevel.ERROR, "Could not archive files. Error message: '" + e.message,
                    this.javaClass.name, "archiveFiles")
            return 0
        }

    }

    /**
     * Method used as a filter to determine, should provided file be archived or not
     * @param file Path to file to check
     * @return True if file should be archived or false otherwise
     */
    open fun checkFile(file: Path): Boolean {
        if (!filterRegex.isEmpty() && !Pattern.compile(filterRegex).matcher(file.toString()).find()) return false
        if (file.toString().endsWith(".tmp")) return false
        if (file.toString() == lastFileName) return false
        if (lastFileTimestamp > 0 && getFileTimestamp(file) < lastFileTimestamp) return false
        if (getFileTimestamp(file) == lastFileTimestamp && file.toString().compareTo(lastFileName) < 0)
            return false
        try {
            if (Files.size(file) == 0L) return false
            val fileSize = Files.size(file)
            if (maxArchiveSize > 0 && archivedFilesSize + fileSize > maxArchiveSize) {
                return false
            }
            if (maxArchiveFilesCount > 0 && archivedFilesCount + 1 > maxArchiveFilesCount) {
                return false
            }
            addArchivedFilesSize(fileSize)
            incArchivedFilesCount()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
    }

    /**
     * Method used as comparator to sort list of files before pass to archive processor
     * @param file1 First file to compare
     * @param file2 Second file to compare
     * @return 0 - if files are equal, >0 if first file greater than second, <0 if first file less than seconf
     */
    private fun sortFiles(file1: Path, file2: Path): Int {
        return java.lang.Long.valueOf(getFileTimestamp(file1)!! - getFileTimestamp(file2)!!).toInt()
    }

    /**
     * Method used to archive file
     * @param sourceFile Path to file to archive
     */
    open fun processFile(sourceFile: Path) {
        processor!!.processFile(sourceFile)
    }

    /**
     * Method called after archiving each file
     * @param sourceFile Path to source file archived
     */
    override fun finishFileProcessing(sourceFile: Path) {
        lastFileName = sourceFile.toString()
        lastFileTimestamp = getFileTimestamp(sourceFile)
        processor!!.finishFileProcessing(sourceFile)
    }

    /**
     * Method used to get timestamp of file
     * @param file - File to get timestamp of
     * @return Timestamp of file
     */
    open fun getFileTimestamp(file: Path): Long {
        try {
            return Files.getLastModifiedTime(file).toMillis() / 1000
        } catch (e: IOException) {
            syslog!!.log(ISyslog.LogLevel.ERROR, "Could not get timestamp of file '" + file.toString() + "'." +
                    "Error message: '" + e.message, this.javaClass.name, "archiveFiles")
            return 0L
        }
    }

    /**
     * Method used to get timestmap of file
     * @param file - File to get timestamp of
     * @param usingPathContents - If true, than function uses components of path to determine timestamp
     * (used for logged data),otherwise it reads last modified time of file
     * @return Timestamp of file
     */
    protected fun getFileTimestamp(file: Path, usingPathContents: Boolean): Long? {
        if (!usingPathContents) return getFileTimestamp(file)
        val parentPath = file.parent
        if (parentPath.nameCount < 6) return 0L
        try {
            val fileName = file.fileName.toString()
            val count = parentPath.nameCount
            val second = Integer.parseInt(fileName.substring(0, fileName.indexOf(".")))
            val minute = Integer.parseInt(parentPath.getName(count - 1).toString())
            val hour = Integer.parseInt(parentPath.getName(count - 2).toString())
            val day = Integer.parseInt(parentPath.getName(count - 3).toString())
            val month = Integer.parseInt(parentPath.getName(count - 4).toString())
            val year = Integer.parseInt(parentPath.getName(count - 5).toString())
            return LocalDateTime.of(year, month, day, hour, minute, second).toEpochSecond(ZoneOffset.UTC)
        } catch (e: Exception) {
            syslog!!.log(ISyslog.LogLevel.ERROR, "Could not get timestamp for path '" + parentPath.toString() + "'. " +
                    "Error message: " + e.message, this.javaClass.name, "getFileTimestamp")
            return 0L
        }

    }

    /**
     * Method used to get destination path which will be used to archive provided source file
     * @param file Path to source file
     * @return Destination path of this file
     */
    override fun getDestinationPathOfFile(file: Path): Path {
        val relativePath = file.toString().replace(sourcePath, "")
        return Paths.get(getDestinationPath() + relativePath)
    }

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    override fun run() {
        super.run()
        archive()
        taskStatus = CronjobTaskStatus.IDLE
        lastExecTime = Instant.now().epochSecond
    }

    override fun getDestinationPath(): String {
        var resultPath = mdestinationPath
        if (resultPath.isEmpty())
            resultPath = LoggerApplication.instance.getCachePath() + "/archivers/" + this.name
        if (!Paths.get(resultPath).isAbsolute)
            resultPath = LoggerApplication.instance.getCachePath() + "/archivers/" + this.name + "/" + mdestinationPath
        return resultPath
    }

    fun incArchivedFilesCount() {
        archivedFilesCount += 1
    }

    fun addArchivedFilesSize(size: Long) {
        archivedFilesSize += size
    }

    companion object {

        /**
         * Factory method, used to build concrete Data Archiver object, based on provided unique name
         * which method will try to find in current configuration
         * @param name Unique name
         * @return Constructed archiver object
         */
        fun create(name: String): IDataArchiver? {
            if (name.isEmpty()) return null
            val config = ConfigManager.getInstance().getDataArchiver(name)
            return create(config)
        }

        /**
         * Factory method, used to build concrete Data Archiver object, based on provided configuration
         * @param config Configuration object
         * @return Constructed archiver object
         */
        fun create(config: HashMap<String, Any>?): IDataArchiver? {
            if (config == null) return null
            val type = (config as java.util.Map<String, Any>).getOrDefault("type", "").toString()
            if (type.isEmpty()) return null
            when (type) {
                "copy" -> return FileCopyDataArchiver(config)
                "zip" -> return FileZipDataArchiver(config)
                "data_copy" -> return DataCopyDataArchiver(config)
                "data_zip" -> return DataZipDataArchiver(config)
                "send_ftp" -> return SendFtpDataArchiver(config)
                else -> return null
            }
        }
    }
}