package archivers.processors

import archivers.IDataArchiver
import main.ISyslog
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.HashMap

/**
 * Archive processor, which sends source files to remote FTP server
 */
class SendFtpArchiveProcessor
/**
 * Class constructor
 * @param archiver - Source Data archiver
 */
internal constructor(archiver: IDataArchiver) : ArchiveProcessor(archiver) {

    // Link to FTP connection
    private val connection = FTPClient()
    // FTP host
    private var host = ""
    // FTP port
    private var port = 21
    // FTP login
    private var username = ""
    // FTP password
    private var password = ""
    // Should initiate passive connection
    private var passiveMode = true
    // Path on FTP to which upload files (relative to user path on server)
    private var rootPath = "/"
    // FTP connection timeout in seconds
    private var connectionTimeout = 30
    // FTP socket timeout in seconds (how much time to wait in case of IDLE or other stuck while uploading file)
    private var socketTimeout = 10

    /// Failure which happened during file processing, which must interrupt stream and stop processing all
    /// files until the end of stream
    private var globalFailure = false

    /**
     * Method used to set parameters of archiver from provided configuration object
     * @param config Configuration object
     */
    override fun configure(config: HashMap<String, Any>) {
        super.configure(config)
        host = (config as java.util.Map<String, Any>).getOrDefault("host", host).toString()
        port = java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("port", port).toString()).toInt()
        username = (config as java.util.Map<String, Any>).getOrDefault("username", username).toString()
        password = (config as java.util.Map<String, Any>).getOrDefault("password", password).toString()
        rootPath = (config as java.util.Map<String, Any>).getOrDefault("rootPath", rootPath).toString()
        passiveMode = java.lang.Boolean.parseBoolean((config as java.util.Map<String, Any>).getOrDefault("passiveMode", passiveMode).toString())
        connectionTimeout = java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("connectionTimeout", connectionTimeout).toString()).toInt() * 1000
        socketTimeout = java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("socketTimeout", socketTimeout).toString()).toInt() * 1000
    }

    /**
     * Method used to initialize archive before starting put files to it
     * @return True if archive initialized successfully or false otherwise
     */
    override fun validateAndInitArchive(): Boolean {
        val sourcePath = archiver.sourcePath
        globalFailure = false
        if (!Files.exists(Paths.get(sourcePath))) return false
        try {
            connection.setDataTimeout(socketTimeout)
            connection.connect(host, port)
            val reply = connection.replyCode
            if (!FTPReply.isPositiveCompletion(reply)) {
                connection.disconnect()
                syslog.log(ISyslog.LogLevel.ERROR, "Could not initiate FTP connection. " +
                        "Error message: " + connection.replyString, this.javaClass.name,
                        "validateAndInitArchive")
                return false
            }
            if (!connection.login(username, password)) {
                syslog.log(ISyslog.LogLevel.ERROR, "Could not initiate FTP connection. " + "Invalid login or password.", this.javaClass.name, "validateAndInitArchive")
                return false
            }
            if (passiveMode) connection.enterLocalPassiveMode()
            if (rootPath != "/") {
                if (!connection.changeWorkingDirectory(rootPath)) {
                    syslog.log(ISyslog.LogLevel.ERROR, "Could not set FTP working directory '" + rootPath + "' ." +
                            "Error message '" + connection.replyString + "'",
                            this.javaClass.name, "validateAndInitArchive")
                    return false
                }
            }
            connection.setFileType(FTP.BINARY_FILE_TYPE)
            connection.connectTimeout = connectionTimeout
        } catch (e: IOException) {
            syslog.log(ISyslog.LogLevel.ERROR, "Could not initiate FTP connection. " +
                    "Error message: " + e.message, this.javaClass.name, "validateAndInitArchive")

        }

        return true
    }

    /**
     * Method puts provided file to archive
     * @param sourceFile Path to source file to place to archive
     */
    override fun processFile(sourceFile: Path) {
        if (globalFailure) return
        try {
            connection.soTimeout = socketTimeout
            if (connection.storeFile(sourceFile.fileName.toString(),
                            FileInputStream(sourceFile.toFile())))
                archiver.finishFileProcessing(sourceFile)
            else {
                syslog.log(ISyslog.LogLevel.ERROR, "Could not upload file by FTP: '" +
                        sourceFile.toString() + "'. Error message: " + connection.replyString,
                        this.javaClass.name, "processFile")
            }
        } catch (e: IOException) {
            globalFailure = true
            syslog.log(ISyslog.LogLevel.ERROR, "Could not upload file by FTP: '" + sourceFile.toString() + "'. " +
                    "Error message: " + e.message, this.javaClass.name, "processFile")
        }

    }

    /**
     * Method called after last file placed to archive
     */
    override fun finish() {
        try {
            connection.disconnect()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}
