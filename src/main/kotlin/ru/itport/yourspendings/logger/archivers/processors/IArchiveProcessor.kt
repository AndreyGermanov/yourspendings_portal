package archivers.processors

import java.nio.file.Path
import java.util.HashMap

/**
 * Interface, which each Archive processor should implement to be included as processor
 * of Data archiver
 */
interface IArchiveProcessor {
    fun configure(config: HashMap<String, Any>)
    fun validateAndInitArchive(): Boolean
    fun processFile(file: Path)
    fun finishFileProcessing(file: Path)
    fun finish()
}
