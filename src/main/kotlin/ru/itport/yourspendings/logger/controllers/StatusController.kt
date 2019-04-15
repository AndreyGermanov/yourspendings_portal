package controllers

import io.javalin.Context
import main.ISyslog
import webservers.IWebServer

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class StatusController : Controller() {

    override fun handlePostRequest(route: String, webserver: IWebServer, ctx: Context) {
        when (route) {
            "/status" -> this.actionPostStatus(ctx, webserver)
        }
    }

    private fun actionPostStatus(ctx: Context, webServer: IWebServer) {
        try {
            ctx.result(BufferedReader(InputStreamReader(ctx.req.inputStream)).readLine())
        } catch (e: IOException) {
            webServer.syslog!!.log(ISyslog.LogLevel.ERROR, "Error while reading request body. " +
                    "Error message: " + e.message, this.javaClass.name, "actionPostStatus")
        }

    }
}
