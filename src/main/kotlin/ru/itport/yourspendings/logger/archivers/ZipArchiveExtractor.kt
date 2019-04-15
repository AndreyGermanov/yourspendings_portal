package archivers

import archivers.processors.ZipArchiveProcessor

import java.nio.file.Path
import java.util.HashMap

/**
 * Class implements specific type of Archiver, which used to extract files from ZIP archives
 * to destination folder. Source folder of this archiver should contain list of ZIP files to extract.
 */
class ZipArchiveExtractor
/**
 * Class constructor
 *
 * @param config Configuration object
 */
(config: HashMap<String, Any>) : FileCopyDataArchiver(config) {

    /**
     * Method used to set parameters of archiver from provied configuration object
     * @param config Configuration object
     */
    override fun configure(config: HashMap<String, Any>?) {
        config!!["type"] = "data_zip"
        super.configure(config)
    }

    /**
     * Method used as a filter to determine, should provided file be archived or not
     * @param sourceFile Path to file to check
     * @return True if file should be archived or false otherwise
     */
    override fun checkFile(sourceFile: Path): Boolean {
        return if (!sourceFile.toString().endsWith(".zip")) false else super.checkFile(sourceFile)
    }

    /**
     * Method used to archive file
     * @param sourceFile Path to file to archive
     */
    override fun processFile(sourceFile: Path) {
        val processor = this.processor as ZipArchiveProcessor?
        processor!!.extractArchive(sourceFile)
        finishFileProcessing(sourceFile)
    }
}