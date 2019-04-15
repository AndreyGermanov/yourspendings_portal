package webservers

import authenticators.IRequestAuthenticator
import authenticators.RequestAuthenticator
import io.javalin.Javalin
import main.ISyslog
import main.LoggerApplication
import main.Syslog
import main.WebService
import java.nio.file.Files
import java.nio.file.Paths
import java.util.HashMap

/**
 * Class which implements WebServer, working over HTTP or HTTPS
 */
class WebServer : IWebServer {

    // Unique name of webserver
    override var name = ""

    // Port on which webserver runs
    private var port = 0
    // Which path web server will use as path with static content (html, css files and images)
    private var staticPath = ""
    // Link to Jetty webserver instance
    private var app: Javalin? = null
    // Link to system logger to log error or warning messages of this server or controllers
    override var syslog: ISyslog? = null

    // Configuration of routes, which webserver can serve
    private var routes = HashMap<String, Any>()

    override var syslogConfig = HashMap<String, Any>()

    private val urls = HashMap<String, Any>()

    private var authenticator: IRequestAuthenticator? = null

    override val syslogPath: String
        get() = LoggerApplication.instance.getLogPath() + "/webservers/" + this.name

    /**
     * Class constructors
     */
    constructor() {}

    constructor(config: HashMap<String, Any>) {
        this.configure(config)
        this.setup()
    }

    override
            /**
             * Method used to set server variables from configuration file
             */
    fun configure(config: HashMap<String, Any>) {
        name = (config as java.util.Map<String, Any>).getOrDefault("name", name).toString()
        port = java.lang.Double.valueOf((config as java.util.Map<String, Any>).getOrDefault("port", port).toString()).toInt()
        staticPath = (config as java.util.Map<String, Any>).getOrDefault("staticPath", staticPath).toString()
        if (syslog == null) syslog = Syslog(this)
        if (config.containsKey("routes") && config["routes"] is HashMap<*, *>) {
            routes = config["routes"] as HashMap<String, Any>
        }
        try {
            syslogConfig = (config as java.util.Map<String, Any>).getOrDefault("syslog",
                    LoggerApplication.instance.syslogConfig) as HashMap<String, Any>
        } catch (e: Exception) {
            e.printStackTrace()
        }

        authenticator = RequestAuthenticator[(config as java.util.Map<String, Any>).getOrDefault("authenticator", "").toString()]
    }

    /**
     * Method used to bind route handlers for all routes, which configured for this webserver
     * in configuration file
     * @param routes Configuration object for routes
     */
    internal fun initRoutes(routes: HashMap<String, Any>) {
        val webService = WebService.getInstance()
        routes.entries.stream().forEach { routeEntry ->
            if (routeEntry.value !is HashMap<*, *>) routes.entries.stream().forEach {
                val route = routeEntry.value as HashMap<String, Any>
                val url = (route as java.util.Map<String, Any>).getOrDefault("url", "").toString()
                if (url.isEmpty()) routes.entries.stream().forEach {
                    urls[url] = route.clone()
                    val requestMethod = (route as java.util.Map<String, Any>).getOrDefault("method", "GET").toString()
                    when (requestMethod) {
                        "GET" -> app!!.get(url) { ctx -> webService.handleRequest(route, this, ctx) }
                        "POST" -> app!!.post(url) { ctx -> webService.handleRequest(route, this, ctx) }
                        "PUT" -> app!!.put(url) { ctx -> webService.handleRequest(route, this, ctx) }
                        "DELETE" -> app!!.delete(url) { ctx -> webService.handleRequest(route, this, ctx) }
                    }
                }
            }
        }
    }

    /**
     * Method used to setup web server instance according to configuration options before start
     */
    override fun setup() {
        app = Javalin.create()
        if (!staticPath.isEmpty() && Files.exists(Paths.get(staticPath).toAbsolutePath())) {
            app!!.enableStaticFiles(staticPath)
        }
        app!!.before { ctx ->
            ctx.header("Access-Control-Allow-Origin", "*")
            ctx.header("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS")
            ctx.header("Access-Control-Allow-Headers", "Authorization,Content-Type")
            ctx.result("")
        }
        app!!.get("/") { ctx -> ctx.result("Server '$name' is listening on port $port") }
        if (routes.size > 0) initRoutes(routes)
    }

    override fun getAuthenticator(url: String): IRequestAuthenticator? {
        if (urls[url] == null || urls[url] !is HashMap<*, *>) return null
        val routeConfig = urls[url] as HashMap<String, Any>
        return if (routeConfig["authenticator"] == null)
            this.authenticator!! else RequestAuthenticator[(routeConfig as java.util.Map<String, Any>).getOrDefault("authenticator", "").toString()]!!
    }

    /**
     * Method used to run web server either directly or inside separate thread (new Thread(webserver) )
     */
    override fun run() {
        app!!.start(port)
    }

}
