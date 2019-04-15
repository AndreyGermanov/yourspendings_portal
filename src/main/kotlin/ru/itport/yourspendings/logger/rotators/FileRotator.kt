package rotators

import config.ConfigManager
import cronjobs.CronjobTask
import cronjobs.CronjobTaskStatus
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.Comparator
import java.util.HashMap
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Class used to implement file rotation. It can be applied to any file, but in most case used
 * to rotate log files.
 */
class FileRotator : CronjobTask, IFileRotator {

    // Path to file which used as source for rotation
    private var filePath: Path? = null
    // Maximum number of archives in rotation
    private var maxArchives = 5
    // Maximum size of source file, if file is bigger than this rotator will rotate it
    private var maxSourceFileSize = 1024L
    // Should source file be removed after rotation
    private var removeSourceFileAfterRotation = false
    // Should archives in rotation be compressed by zip
    private var compressArchives = false
    // Unique name of this file rotator
    override var name = ""

    // Extension of archive file
    private var fileExt = ".zip"

    /**
     * Method used to get list of archives, created by this rotator
     * @return List of paths of archives
     */
    val archives: List<Path>
        @Throws(IOException::class)
        get() = Files.walk(filePath!!.parent, 1)
                .filter { path -> path.fileName.toString().startsWith(filePath!!.fileName.toString()) && path.toString().endsWith(fileExt) }
                .sorted()
                .collect(Collectors.toList())

    override var lastRecord: Any? = null

    override var lastRecordTimestamp: Long = 0L

    /**
     * Returns a type of collection of tasks, to which current task belongs (loggers, aggregators, archivers etc)
     * @return Collection name as string
     */
    override val collectionType: String
        get() = "rotators"

    /**
     * Class constructor
     * @param name Name of rotator in configuration file
     */
    constructor(name: String) {
        this.configure(ConfigManager.getInstance().getConfigNode("rotators", name))
    }

    /**
     * Class constructor
     * @param config Configuration object for rotator
     */
    constructor(config: HashMap<String, Any>) {
        this.configure(config)
    }

    /**
     * Method used to apply configuration to object.
     * @param config: Configuration object
     */
    override fun configure(config: HashMap<String, Any>?) {
        if (config == null || !config.containsKey("filePath")) return
        super.configure(config)
        name = (config as java.util.Map<String, Any>).getOrDefault("name", name).toString()
        filePath = Paths.get((config as java.util.Map<String, Any>).getOrDefault("filePath", filePath).toString())
        maxArchives = java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("maxArchives", maxArchives).toString()).toInt()
        maxSourceFileSize = java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("maxSourceFileSize", maxSourceFileSize).toString()).toLong()
        removeSourceFileAfterRotation = java.lang.Boolean.parseBoolean(
                (config as java.util.Map<String, Any>).getOrDefault("removeSourceFileAfterRotation", removeSourceFileAfterRotation).toString()
        )
        compressArchives = java.lang.Boolean.parseBoolean((config as java.util.Map<String, Any>).getOrDefault("compressArchives", compressArchives).toString())
        fileExt = if (compressArchives) ".zip" else ".bak"
    }

    /**
     * Method, which Timer used to run this object as a Cronjob ("TimerTask" implementation)
     */
    override fun run() {
        super.run()
        rotate()
        taskStatus = CronjobTaskStatus.IDLE
        lastExecTime = Instant.now().epochSecond
    }

    /**
     * Main method, which begins rotation process
     */
    override fun rotate() {
        try {
            if (Files.notExists(filePath) || Files.size(filePath!!) < maxSourceFileSize) return
            rotateArchives()
            createArchive()
            if (removeSourceFileAfterRotation) Files.delete(filePath!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    /**
     * Method rotates all previous archives (removes oldest one and moves all archives back by one position)
     */
    @Throws(IOException::class)
    internal fun rotateArchives() {
        var logFiles = archives
        val numberFormat = "%0" + maxArchives.toString().length + "d"
        if (logFiles.size >= maxArchives) {
            logFiles.sortedWith(Comparator.reverseOrder())
            for (start in 0..logFiles.size - maxArchives)
                Files.deleteIfExists(logFiles[start])
            logFiles = archives
        }
        if (logFiles.isNotEmpty()) {
            for (start in logFiles.size downTo 1) {
                Files.move(logFiles[start - 1],
                        Paths.get(filePath!!.parent.toString() + "/" +
                                filePath!!.fileName.toString() + "_" + String.format(numberFormat, start + 1) + fileExt),
                        StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    /**
     * Method used to create new archive from source file
     */
    @Throws(IOException::class)
    internal fun createArchive() {
        val numberFormat = "%0" + maxArchives.toString().length + "d"
        val archivePath = filePath!!.parent.toString() + "/" + filePath!!.fileName.toString() +
                "_" + String.format(numberFormat, 1) + fileExt
        if (compressArchives) createArchiveZip(archivePath) else createArchiveBak(archivePath)
    }

    /**
     * Method used to create compressed archive
     * @param archivePath
     */
    @Throws(IOException::class)
    internal fun createArchiveZip(archivePath: String) {
        val out = ZipOutputStream(FileOutputStream(archivePath))
        out.putNextEntry(ZipEntry(filePath!!.fileName.toString()))
        val buffer = ByteArray(1024)
        val inp = FileInputStream(filePath!!.toString())
        var length: Int = inp.read(buffer)
        while (length > 0) { out.write(buffer, 0, length); length = inp.read(buffer)}
        out.closeEntry()
        out.close()
    }

    /**
     * Method used to create uncompressed archive
     * @param archivePath
     */
    @Throws(IOException::class)
    internal fun createArchiveBak(archivePath: String) {
        Files.copy(filePath!!, Paths.get(archivePath), StandardCopyOption.REPLACE_EXISTING)
    }

}