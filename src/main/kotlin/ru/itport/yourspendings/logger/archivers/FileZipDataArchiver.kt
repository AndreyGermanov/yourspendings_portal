package archivers

import java.util.HashMap

/**
 * Data archiver which archives file by creating ZIP archives in destination folder
 */
open class FileZipDataArchiver
/**
 * Class constructor
 * @param config - Configuration object
 */
internal constructor(config: HashMap<String, Any>) : DataArchiver(config)