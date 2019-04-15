package controllers

import io.javalin.Context
import webservers.IWebServer

/**
 * Interface which all controllers should implement
 */
interface IController {
    fun handleRequest(route: String, webServer: IWebServer, ctx: Context)
}
