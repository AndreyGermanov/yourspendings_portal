package archivers.processors

import archivers.IDataArchiver
import main.ISyslog
import java.io.IOException
import java.nio.file.*

/**
 * Archive processor, which used to archive files by just copying them to destination folder,
 * provided by Data archiver
 */
class CopyArchiveProcessor
/**
 * Class consturctor
 * @param archiver - Source data Archiver
 */
internal constructor(archiver: IDataArchiver) : ArchiveProcessor(archiver) {

    /**
     * Method puts provided file to archive
     * @param sourceFile Path to source file to place to archive
     */
    override fun processFile(sourceFile: Path) {
        val destinationFile = archiver.getDestinationPathOfFile(sourceFile)
        try {
            if (!Files.exists(destinationFile.parent)) Files.createDirectories(destinationFile.parent)
            val tmpDestinationFile = Paths.get(destinationFile.toString() + ".tmp")
            Files.copy(sourceFile, tmpDestinationFile, StandardCopyOption.REPLACE_EXISTING)
            Files.move(tmpDestinationFile, destinationFile, StandardCopyOption.REPLACE_EXISTING)
            archiver.finishFileProcessing(sourceFile)
        } catch (e: IOException) {
            syslog.log(ISyslog.LogLevel.ERROR, "Could not copy file '" + destinationFile.toString() + "'." +
                    "Error message+'" + e.message + "'", this.javaClass.name, "processFile")
        }

    }
}