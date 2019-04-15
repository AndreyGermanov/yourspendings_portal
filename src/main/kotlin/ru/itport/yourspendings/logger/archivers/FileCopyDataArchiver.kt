package archivers

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.HashMap

/**
 * Data archiver which archives file by just copying it from source to destination folder
 */
open class FileCopyDataArchiver
/**
 * Class constructor
 * @param config Configuration object
 */
internal constructor(config: HashMap<String, Any>) : DataArchiver(config) {

    // Determines how to process files, which already exists in destination folder (skip, overwrite, overwrite if newer)
    private var fileUpdateRule = FileUpdateRule.OVERWRITE_IF_NEW

    /**
     * Method used to set parameters of archiver from provied configuration object
     * @param config Configuration object
     */
    override fun configure(config: HashMap<String, Any>?) {
        super.configure(config)
        val overwriteFiles = (config as java.util.Map<String, Any>).getOrDefault("overwriteFiles", "").toString()
        when (overwriteFiles) {
            "OVERWRITE" -> fileUpdateRule = FileUpdateRule.OVERWRITE
            "OVERWRITE_IF_NEW" -> fileUpdateRule = FileUpdateRule.OVERWRITE_IF_NEW
            "SKIP" -> fileUpdateRule = FileUpdateRule.SKIP
        }
    }

    /**
     * Method used as a filter to determine, should provided file be archived or not
     * @param file Path to file to check
     * @return True if file should be archived or false otherwise
     */
    override fun checkFile(file: Path): Boolean {
        if (!super.checkFile(file)) return false
        val destinationFile = getDestinationPathOfFile(file)
        try {
            if (!Files.exists(destinationFile)) return true
            when (fileUpdateRule) {
                FileUpdateRule.OVERWRITE -> return true
                FileUpdateRule.OVERWRITE_IF_NEW -> return Files.getLastModifiedTime(file).toMillis() > Files.getLastModifiedTime(destinationFile).toMillis()
                else -> return false
            }
        } catch (e: IOException) {
            return false
        }

    }
}

/**
 * List of all possible file update rules (used by archiver to determine what to do with
 * source files when they already exist in destination folder)
 */
internal enum class FileUpdateRule {
    OVERWRITE, OVERWRITE_IF_NEW, SKIP
}