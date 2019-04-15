package db.adapters

import java.sql.DriverManager
import java.sql.SQLException
import java.util.HashMap

/**
 * Database adapter for SQLite databases
 */
class SqliteDatabaseAdapter : JDBCDatabaseAdapter() {

    // Path to database file
    private var path = ""

    /**
     * Method used to apply configuration to data adapter
     * @param config Configuration object
     */
    override fun configure(config: HashMap<String, Any>) {
        super.configure(config)
        if (config == null) return
        this.path = (config as java.util.Map<String, Any>).getOrDefault("path", "").toString()
    }

    /**
     * Method used to open database connection (which is previously setup adn configured)
     */
    override fun connect() {
        val url = "jdbc:sqlite://$path"
        try {
            this.connection = DriverManager.getConnection(url)
        } catch (e: SQLException) {
            syslog.logException(e, this, "connect")
        }

    }
}