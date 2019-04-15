package archivers

import cronjobs.ICronjobTask
import main.ISyslog
import java.nio.file.Path

/**
 * Interface which each Data Archiver must implement
 */
interface IDataArchiver : ICronjobTask {
    val sourcePath: String
    val mdestinationPath: String
    val syslog: ISyslog?
    val removeSourceAfterArchive: Boolean
    val archivedFilesCount: Long
    fun archive(): Long
    fun finishFileProcessing(sourceFile: Path)
    fun getDestinationPathOfFile(sourceFile: Path): Path
    fun getDestinationPath():String
}