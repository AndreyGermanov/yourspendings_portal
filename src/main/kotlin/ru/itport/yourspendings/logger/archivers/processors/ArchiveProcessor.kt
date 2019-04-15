package archivers.processors

import archivers.IDataArchiver
import main.ISyslog
import utils.FileUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.HashMap

/**
 * Base class for archive processors. Archive processor works in composition with Data Archiver, it used
 * to get source files from data archiver and do actual work to put them to archiver
 */
abstract class ArchiveProcessor : IArchiveProcessor {

    // Link to DataArchiver object, which used as a source of files to archive
    protected lateinit var archiver: IDataArchiver
    // Link ty syslog object, used to write errors and warnings
    protected lateinit var syslog: ISyslog

    /**
     * Class consturctor
     * @param archiver - Source data archiver
     */
    internal constructor(archiver: IDataArchiver) {
        this.archiver = archiver
        syslog = archiver.syslog!!
    }

    internal constructor() {}

    /**
     * Method used to initialize archive before starting put files to it
     * @return True if archive initialized successfully or false otherwise
     */
    override fun validateAndInitArchive(): Boolean {
        val sourcePath = archiver.sourcePath
        val destinationPath = archiver.mdestinationPath

        if (!Files.exists(Paths.get(sourcePath))) return false
        try {
            if (!Files.exists(Paths.get(destinationPath))) Files.createDirectories(Paths.get(destinationPath))
            if (!Files.exists(Paths.get(destinationPath))) return false
        } catch (e: IOException) {
            syslog.log(ISyslog.LogLevel.ERROR, "Could not create destinationPath '" + destinationPath + "'." +
                    "Error message+'" + e.message + "'", this.javaClass.name, "validateConfig")
            return false
        }

        return true
    }

    /**
     * Method called by archiver after each file placed to archive
     * @param file - Source file, which placed to archive
     */
    override fun finishFileProcessing(file: Path) {
        if (!archiver.removeSourceAfterArchive) return
        try {
            Files.deleteIfExists(file)
        } catch (e: IOException) {
            syslog.log(ISyslog.LogLevel.ERROR, "Could not remove source file '" + file.toString() + "'." +
                    "Error message+'" + e.message + "'", this.javaClass.name, "finishFileProcessing")
        }

    }

    /**
     * Method used to set parameters of archiver from provied configuration object
     * @param config Configuration object
     */
    override fun configure(config: HashMap<String, Any>) {}

    /**
     * Method called after last file placed to archive
     */
    override fun finish() {
        if (archiver.removeSourceAfterArchive)
            FileUtils.removeFolder(Paths.get(archiver.sourcePath), true)
    }

    companion object {

        /**
         * Factory method used to construct Archive processors of concrete type from configuration file
         * @param type Type of archiver
         * @param archiver - Link to archiver object
         * @return
         */
        fun create(type: String, archiver: IDataArchiver): IArchiveProcessor? {
            when (type) {
                "copy", "data_copy" -> return CopyArchiveProcessor(archiver)
                "zip", "data_zip" -> return ZipArchiveProcessor(archiver)
                "send_ftp" -> return SendFtpArchiveProcessor(archiver)
                else -> return null
            }
        }
    }
}