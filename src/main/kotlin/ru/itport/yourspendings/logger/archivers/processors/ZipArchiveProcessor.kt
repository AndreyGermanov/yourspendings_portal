package archivers.processors

import archivers.IDataArchiver
import archivers.ZipArchiveExtractor
import main.ISyslog

import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Archive processor, which archives source files to ZIP archive
 */
class ZipArchiveProcessor
/**
 * Class constructor
 * @param archiver - Source Data archiver
 */
internal constructor(archiver: IDataArchiver) : ArchiveProcessor(archiver) {

    // Link to Zip archive object
    internal lateinit var archive: ZipOutputStream
    // Full path and name of destination ZIP archive
    internal var archiveName = ""

    /**
     * Method returns full path to created archive without extension
     * @return Path to archive
     */
    val archivePath: String
        get() = archiver.mdestinationPath + "/" + getArchiveName()

    /**
     * Method returns file name of ZIP archive with extension
     * @return File name
     */
    val archiveFileName: String
        get() = getArchiveName() + ".zip"

    /**
     * Method returns full path to created ZIP archive with extension
     * @return Path to file
     */
    val archiveFilePath: String
        get() = "$archivePath.zip"

    /**
     * Method used to initialize archive before starting put files to it
     * @return True if archive initialized successfully or false otherwise
     */
    override fun validateAndInitArchive(): Boolean {
        if (archiver is ZipArchiveExtractor) return true
        if (!super.validateAndInitArchive()) return false
        try {
            archiveName = ""
            archive = ZipOutputStream(FileOutputStream("$archiveFilePath.tmp"))
            return true
        } catch (e: IOException) {
            syslog.log(ISyslog.LogLevel.ERROR, "Could not create archive '" + archiveFilePath + "'. " +
                    "Error message: " + e.message, this.javaClass.name, "validatedAndInitArchive")
            return false
        }

    }

    /**
     * Method puts provided file to archive
     * @param sourceFile Path to source file to place to archive
     */
    override fun processFile(sourceFile: Path) {
        try {
            Files.newInputStream(sourceFile).use { stream ->
                val entry = ZipEntry(sourceFile.toString().replace(archiver.sourcePath + "/", ""))
                if (Files.isDirectory(sourceFile)) return
                archive.putNextEntry(entry)

                val bufSize = 1024
                val buf = ByteArray(bufSize)
                var length: Int = stream.read(buf)
                while (length > 0) {
                    archive.write(buf, 0, length)
                    length = stream.read(buf)
                }
                archive.closeEntry()
                archiver.finishFileProcessing(sourceFile)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            syslog.log(ISyslog.LogLevel.ERROR, "Could not add file '" + sourceFile.toString() + "', to archive. " +
                    "Error message: " + e.message, this.javaClass.name, "processFile")
        }

    }

    /**
     * Method called after last file placed to archive
     */
    override fun finish() {
        if (archiver is ZipArchiveExtractor) return
        try {
            if (archiver.archivedFilesCount > 0) {
                archive.close()
                Files.move(Paths.get("$archiveFilePath.tmp"), Paths.get(archiveFilePath))
            } else {
                archive.close()
                Files.deleteIfExists(Paths.get("$archiveFilePath.tmp"))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            syslog.log(ISyslog.LogLevel.ERROR, "Could not finish writing archive file '" + archiveFilePath + ". " +
                    "Error message: " + e.message, this.javaClass.name, "finish")
        }

    }

    /**
     * Utility method used by ZipArchiveExtractor to extract ZIP archive to destination folder
     * @param zipFile Source ZIP file to extract
     * @return Extracted files count
     */
    fun extractArchive(zipFile: Path): Long {
        var extractedFilesCount = 0L
        try {
            ZipInputStream(FileInputStream(zipFile.toString())).use { input ->
                var entry: ZipEntry? = input.nextEntry
                val buffer = ByteArray(1024)
                while (entry != null) {
                    val fullPath = Paths.get(archiver.mdestinationPath + "/" + entry.name)
                    if (!Files.exists(fullPath.parent)) Files.createDirectories(fullPath.parent)
                    val os = FileOutputStream(fullPath.toFile())
                    var len: Int = input.read(buffer)
                    while (len > 0) {
                        os.write(buffer, 0, len)
                        len = input.read(buffer)
                    }
                    os.close()
                    extractedFilesCount++
                    entry = input.nextEntry
                }
            }
        } catch (e: IOException) {
            syslog.log(ISyslog.LogLevel.ERROR, "Could not extract zip file '" + zipFile.toString() + ". " +
                    "Error message: " + e.message, this.javaClass.name, "extractArchive")
        }

        return extractedFilesCount
    }

    /**
     * Method returns file name of ZIP archive which is created without extension
     * @return File name
     */
    fun getArchiveName(): String {
        if (archiveName.isEmpty()) {
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            archiveName = archiver.name + "_" + LocalDateTime.now().format(fmt)
        }
        return archiveName
    }
}