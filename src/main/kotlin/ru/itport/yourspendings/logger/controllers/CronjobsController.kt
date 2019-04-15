package controllers

import cronjobs.ICronjobTask
import io.javalin.Context
import main.LoggerService
import webservers.IWebServer
import java.util.HashMap
import java.util.Optional
import java.util.stream.Collectors

class CronjobsController : Controller() {

    /**
     * Uniform method to handle all GET requests, coming to this controller
     * @param route Request URL
     * @param webServer Link to webserver, which received request
     * @param ctx Request context
     */
    override fun handleGetRequest(route: String, webServer: IWebServer, ctx: Context) {
        when (route) {
            //"/cronjobs" -> actionGetCronjobs(webServer, ctx)
            "/cronjobs/types" -> actionGetCronjobTypes(webServer, ctx)
            "/cronjobs/last_record/:cronjob_id" -> actionGetLastRecord(webServer, ctx)
            "/cronjobs/enable/:cronjob_id/:enable" -> actionEnableCronjob(webServer, ctx)
        }
    }

    /**
     * Action which returns list of cronjobs
     * @param webServer Link to webserver
     * @param ctx Request context
     */
    /*
    private fun actionGetCronjobs(webServer: IWebServer, ctx: Context) {
        val cronjobs = loggerService.cronjobNames.stream()
                .collect(Collectors.toMap(
                        { name:String -> name },
                        { this.getCronjobInfo(it).get() },
                        { n1:String, n2:String -> n1 },
                        { HashMap<String,String>() }))
        ctx.res.status = 200
        ctx.result(gson.toJson(cronjobs))
    }
    */
    /**
     * Action which returns last result record, which specified cronjob done
     * @param webServer Link to webserver
     * @param ctx Request context
     */
    private fun actionGetLastRecord(webServer: IWebServer, ctx: Context) {
        val cronjob_id = ctx.pathParam("cronjob_id")
        if (cronjob_id.isEmpty()) {
            sendErrorResponse(ctx, webServer, "Cronjob ID not specified")
            return
        }
        val cronjob = loggerService.getCronjobTask(cronjob_id)
        if (cronjob == null) {
            sendErrorResponse(ctx, webServer, "Cronjob with specified ID not found")
            return
        }
        sendSuccessResponse(ctx, webServer, cronjob!!.lastRecord!!)
    }

    /**
     * Action used to enable/disable specified cronjob
     * @param webServer Link to webserver
     * @param ctx Request context
     */
    private fun actionEnableCronjob(webServer: IWebServer, ctx: Context) {
        val cronjob_id = ctx.pathParam("cronjob_id")
        val enableString = ctx.pathParam("enable")
        var enable: Boolean? = null
        if (enableString == "0") enable = false
        if (enableString == "1") enable = true
        if (enable == null) {
            sendErrorResponse(ctx, webServer, "Incorrect action value")
            return
        }
        val task = LoggerService.getInstance().getCronjobTask(cronjob_id)
        if (task == null) {
            sendErrorResponse(ctx, webServer, "Cronjob with specified ID not found")
            return
        }
        task!!.isEnabled = enable
        sendSuccessResponse(ctx, webServer, null!!)
    }

    /**
     * Action returns list of all possible cronjob types
     * @param webServer Link to webserver
     * @param ctx Request context
     */
    fun actionGetCronjobTypes(webServer: IWebServer, ctx: Context) {
        sendSuccessResponse(ctx, webServer, LoggerService.getInstance().cronjobTypes)
    }

    /**
     * Internal method, which returns status information about cronjob with specified ID
     * @param name ID of cronjob
     * @return HashMap with information about cronjob: name, is it enabled or not, active or not etc.
     */
    private fun getCronjobInfo(name: String): Optional<HashMap<String, Any>> {
        val cronjob = loggerService.getCronjobTask(name) ?: return Optional.empty()
        return Optional.of(hashMapOf("name" to cronjob.name, "status" to cronjob.taskStatus,
                "type" to cronjob.collectionType, "enabled" to
                cronjob.isEnabled, "lastRunTimestamp" to cronjob.lastExecTime!!.toString()))
    }
}
