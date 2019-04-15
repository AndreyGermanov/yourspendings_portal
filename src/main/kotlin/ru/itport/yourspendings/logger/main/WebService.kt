package main

import config.ConfigManager
import controllers.CronjobsController
import controllers.IController
import controllers.StatusController
import io.javalin.Context
import webservers.IWebServer
import webservers.WebServer

import java.util.HashMap

/**
 * Singleton object, used to start all web servers, enabled in configuration files
 */
class WebService
/**
 * Private constructor
 */
private constructor() {

    /// Determines if service already started
    private var started = false
    /// Array of started service cronjobs indexed by names
    private val webservers = HashMap<String, IWebServer>()
    private val controllers = HashMap<String, IController>()

    /// Link to configuration manager, which provides configuration objects for cronjobs
    private val configManager = ConfigManager.getInstance()

    /**
     * Entry point method
     */
    fun start() {
        if (started) return
        registerControllers()
        startWebServers()
        started = true
    }

    /**
     * Method which goes through collection of web server configurations
     * and invokes start method for each item
     */
    private fun startWebServers() {
        val configArray = configManager.getConfigCollection("webservers")
        for (name in configArray!!.keys) {
            startWebServer(configManager.getConfigNode("webservers", name)!!)

        }
    }

    /**
     * Method instantiates, configures and starts webserver using provided config in separate Thread
     * @param config: Configuration object
     */
    private fun startWebServer(config: HashMap<String, Any>) {
        if (!config.containsKey("name") || config["name"].toString().isEmpty()) return
        if (!config.containsKey("enabled") || !java.lang.Boolean.parseBoolean((config as java.util.Map<String, Any>).getOrDefault("enabled", false).toString())) return
        val webserver = WebServer(config)
        webserver.setup()
        webservers[config["name"].toString()] = webserver
        Thread(webserver).start()
    }

    /**
     * Method used to create instances and register all controllers,
     * which can be used to handle requests to webservers
     */
    private fun registerControllers() {
        controllers[CronjobsController::class.java.name] = CronjobsController()
        controllers[StatusController::class.java.name] = StatusController()
    }

    /**
     * Method which webserver calls when receive request to find controller which is responsible to
     * handle this request and execute action on this controller to handle this request
     * @param routeConfig: Config of route, on which webserver responded (from config file)
     * @param webServer: Link to webserver instance, which received request
     * @param ctx: Link to request context, contains all request data and link to response object
     */
    fun handleRequest(routeConfig: HashMap<String, Any>, webServer: IWebServer, ctx: Context) {
        val url = routeConfig["url"].toString()
        if (url.isEmpty()) return
        if (routeConfig.containsKey("controller")) {
            controllers["controller"]!!.handleRequest(url, webServer, ctx)
            return
        }
        controllers.values.stream().forEach { controller -> controller.handleRequest(url, webServer, ctx) }
    }

    companion object {

        private var instance: WebService? = null

        /**
         * Method used to get instance of service from other classes.
         * @return Instance of application
         */
        fun getInstance(): WebService {
            if (instance == null) instance = WebService()
            return instance!!
        }
    }
}
