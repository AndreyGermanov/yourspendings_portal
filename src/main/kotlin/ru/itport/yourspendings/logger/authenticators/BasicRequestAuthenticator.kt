package authenticators

import db.adapters.DatabaseAdapter
import db.adapters.IDatabaseAdapter
import io.javalin.Context
import utils.HashUtils
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Base64
import java.util.HashMap

/**
 * HTTP authenticator which implements HTTP BASIC authentication
 */
class BasicRequestAuthenticator
/**
 * Class constructor
 * @param config Configuration object for authenticator
 */
(config: HashMap<String, Any>) : RequestAuthenticator() {

    // Unique name of authenticator
    private var name = ""
    // Database adapter for database with user credentials
    private var dbAdapter: IDatabaseAdapter? = null
    // Collection (table) in database, which contains user credentials
    private var authCollectionName = ""
    // Field name in collection with "login" of user
    private var usernameField = "email"
    // Field name in collection with password hash of user
    private var passwordField = "password"
    // Hashing algo, used to create password hash
    private var hashAlgo = "SHA-512"

    init {
        this.configure(config)
    }

    /**
     * Method used to apply configuration to object.
     * @param config: Configuration object
     */
    override fun configure(config: HashMap<String, Any>) {
        name = (config as java.util.Map<String, Any>).getOrDefault("name", name).toString()
        dbAdapter = DatabaseAdapter.get((config as java.util.Map<String, Any>).getOrDefault("dbAdapter", "").toString())
        authCollectionName = (config as java.util.Map<String, Any>).getOrDefault("authCollectionName", authCollectionName).toString()
        usernameField = (config as java.util.Map<String, Any>).getOrDefault("usernameField", usernameField).toString()
        passwordField = (config as java.util.Map<String, Any>).getOrDefault("passwordField", passwordField).toString()
        hashAlgo = (config as java.util.Map<String, Any>).getOrDefault("hashAlgo", hashAlgo).toString()
    }

    /**
     * Main method, which authenticates request.
     * @param ctx Http server context of request to authenticate
     * @return True if request authenticated and false otherwise
     */
    override fun authenticate(ctx: Context): Boolean {
        if (dbAdapter == null) return false
        val user = UserCredentials(ctx)
        if (user.username == null) return false
        val sql = "SELECT * FROM " + authCollectionName + " WHERE " + usernameField + "='" + user.username + "' AND " +
                passwordField + "='" + HashUtils.hashString(hashAlgo, user.password) + "'"
        val result = dbAdapter!!.select(sql, null)
        return result != null && result!!.size > 0
    }

    /**
     * Method sends ACCESS DENIED response back to client
     * @param ctx Client request context
     */
    override fun sendDenyResponse(ctx: Context) {
        ctx.res.addHeader("WWW-Authenticate", "Basic realm=\"Auth\"")
        try {
            ctx.res.sendError(401)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Utility class used to work with user credentials
     */
    private inner class UserCredentials {
        // User credentials
        var username: String = ""
        var password: String = ""

        /**
         * Class constructor
         * @param username User name
         * @param password Password
         */
        internal constructor(username: String, password: String) {
            this.username = username
            this.password = password
        }

        /**
         * Class constructor
         * @param ctx Request context which is used to extract username and password from "Authorization" header
         */
        internal constructor(ctx: Context) {
            try {
                val authHeader = ctx.req.getHeader("Authorization")
                if (!authHeader.startsWith("Basic")) return
                val authCredentials = authHeader.replace("Basic ", "")
                val plainText = base64decode(authCredentials.toByteArray())
                val parts = plainText.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (parts.size != 2) return
                this.username = parts[0]
                this.password = parts[1]
            } catch (e: Exception) {
            }

        }

        /**
         * Method returns string representation of this object
         * @return
         */
        override fun toString(): String {
            return this.username + ":" + this.password
        }

        /**
         * Method used to BASE64 encode username and password in format "username:password" for Authorization header
         * @return Base64 encoded string with username and password
         * @throws Exception Method can throw exceptions
         */
        @Throws(Exception::class)
        fun base64encode(): String {
            return BufferedReader(
                    InputStreamReader(
                            ByteArrayInputStream(
                                    Base64.getEncoder().encode((this.username + ":" + this.password).toByteArray())
                            )
                    )
            ).readLine()
        }

        /**
         * Method used to decode Base64 bytes to string
         * @param bytes Incoming Base64 string
         * @return Decoded string
         * @throws Exception Method can throw exceptions
         */
        @Throws(Exception::class)
        fun base64decode(bytes: ByteArray): String {
            return BufferedReader(
                    InputStreamReader(
                            ByteArrayInputStream(Base64.getDecoder().decode(bytes))
                    )
            ).readLine()
        }
    }
}
