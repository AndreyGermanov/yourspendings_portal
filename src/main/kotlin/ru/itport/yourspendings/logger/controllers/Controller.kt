package controllers

import authenticators.IRequestAuthenticator
import com.google.gson.Gson
import io.javalin.Context
import main.LoggerService
import webservers.IWebServer

import java.util.HashMap

/**
 * Base class for Web server controllers. It used to receive requests, call actions and
 * write response to user
 */
open class Controller : IController {

    // JSON serializer instance
    protected var gson = Gson()
    // Link to Logging service, which provides access to cronjobs
    protected var loggerService = LoggerService.getInstance()

    protected var auth: IRequestAuthenticator? = null

    /**
     * Method used to handle requests of various types
     * @param route Route (URL of request)
     * @param webServer Link to webserver from which request came
     * @param ctx Request context, contains all request data and link to Response object to write response too
     */
    override fun handleRequest(route: String, webServer: IWebServer, ctx: Context) {
        auth = webServer.getAuthenticator(route)

        if (auth != null && !auth!!.authenticate(ctx)) {
            auth!!.sendDenyResponse(ctx)
            return
        }

        when (ctx.req.method) {
            "GET" -> this.handleGetRequest(route, webServer, ctx)
            "POST" -> this.handlePostRequest(route, webServer, ctx)
            "PUT" -> this.handlePutRequest(route, webServer, ctx)
            "DELETE" -> this.handleDeleteRequest(route, webServer, ctx)
        }
    }

    /**
     * Uniform method to send error responses to calling HTTP client
     * @param ctx HTTP request context
     * @param message String message
     */
    protected fun sendErrorResponse(ctx: Context, webServer: IWebServer, message: String) {
        ctx.res.status = 500
        val response = hashMapOf("status" to "error")
        if (!message.isEmpty()) response.put("message", message)
        ctx.result(gson.toJson(response))
    }

    /**
     * Uniform method to send success responses to calling HTTP client
     * @param ctx HTTP request context
     * @param result Data to send to calling client
     */
    protected fun sendSuccessResponse(ctx: Context, webServer: IWebServer, result: Any) {
        ctx.res.status = 200
        ctx.result(gson.toJson(hashMapOf("status" to "ok", "result" to result)))
    }

    /**
     * Methods to handle requests of different methods: GET, POST, PUT, DELETE
     */

    open fun handleGetRequest(route: String, webServer: IWebServer, ctx: Context) {}

    open fun handlePostRequest(route: String, webServer: IWebServer, ctx: Context) {}
    open fun handlePutRequest(route: String, webServer: IWebServer, ctx: Context) {}
    open fun handleDeleteRequest(route: String, webServer: IWebServer, ctx: Context) {}

}
