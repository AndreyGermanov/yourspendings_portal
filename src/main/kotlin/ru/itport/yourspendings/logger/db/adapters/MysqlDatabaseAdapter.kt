package db.adapters

import java.sql.*
import java.util.HashMap

/**
 * Database adapter for MySQL databases
 */
class MysqlDatabaseAdapter : JDBCDatabaseAdapter() {

    // Connection credentials

    private var host = ""
    private var port = ""
    private var username = ""
    private var password = ""
    private var database = ""

    /**
     * Method used to apply configuration to data adapter
     * @param config Configuration object
     */
    override fun configure(config: HashMap<String, Any>) {
        super.configure(config)
        if (config == null) return
        this.host = (config as java.util.Map<String, Any>).getOrDefault("host", "").toString()
        this.port = (config as java.util.Map<String, Any>).getOrDefault("port", "").toString()
        this.username = (config as java.util.Map<String, Any>).getOrDefault("username", "").toString()
        this.password = (config as java.util.Map<String, Any>).getOrDefault("password", "").toString()
        this.database = (config as java.util.Map<String, Any>).getOrDefault("database", "").toString()
    }

    /**
     * Method used to open database connection (which is previously setup adn configured)
     */
    override fun connect() {
        val url = "jdbc:mysql://$host:$port/$database?serverTimezone=UTC"
        try {
            this.connection = DriverManager.getConnection(url, username, password)
        } catch (e: SQLException) {
            syslog.logException(e, this, "connect")
        }

    }
}