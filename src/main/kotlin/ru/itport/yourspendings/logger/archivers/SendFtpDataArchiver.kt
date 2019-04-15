package archivers

import java.util.HashMap

/**
 * Data archiver which sends files to remote FTP server
 */
class SendFtpDataArchiver
/**
 * Class constructor
 *
 * @param config Configuration object
 */
internal constructor(config: HashMap<String, Any>) : FileCopyDataArchiver(config) {

    /**
     * Method used to set parameters of archiver from provided configuration object
     * @param config Configuration object
     */
    override fun configure(config: HashMap<String, Any>?) {
        config!!["type"] = "send_ftp"
        super.configure(config)
    }
}