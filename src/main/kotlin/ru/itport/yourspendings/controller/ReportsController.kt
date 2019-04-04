@file:Suppress("UNCHECKED_CAST")

package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.controller.reports.*
import ru.itport.yourspendings.entity.Report
import java.util.*

@RestController
@RequestMapping("/api/report")
class ReportsController:EntityController<Report>("Report") {

    override fun postProcessListItem(item: Report): Any {
        return hashMapOf(
                "uid" to item.uid,
                "name" to item.name,
                "postScript" to item.postScript,
                "queries" to item.queries?.map {
                    hashMapOf(
                            "name" to it.name,
                            "enabled" to it.enabled,
                            "order" to it.order,
                            "query" to it.query,
                            "params" to it.params,
                            "outputFormat" to it.outputFormat,
                            "postScript" to it.postScript,
                            "eventHandlers" to it.eventHandlers,
                            "layout" to it.layout,
                            "report" to it.report!!.uid
                    )
                }
        )
    }

    override fun getItemId(id: Any): Any = id.toString().toIntOrNull() ?: 0

    @PostMapping("/generate")
    fun generate(@RequestBody body:Any=ArrayList<MutableMap<String,Any>>()):Any? {
        val requests = ReportsRequestParser(body,entityManager).apply { parse() }.queries
        return ReportsBuilder(entityManager,requests).apply { build() }.reports
    }

}

