package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.Report

@RestController
@RequestMapping("/api/report")
class ReportsController:EntityController<Report>("Report") {

    override fun postProcessListItem(item: Report): Any {
        return hashMapOf(
                "uid" to item.uid,
                "name" to item.name,
                "queries" to item.queries?.map {
                    hashMapOf(
                            "name" to it.name,
                            "enabled" to it.enabled,
                            "order" to it.order,
                            "query" to it.query,
                            "params" to it.params,
                            "outputFormat" to it.outputFormat,
                            "report" to it.report!!.uid
                    )
                }
        )
    }

    override fun getItemId(id: Any): Any = id.toString().toIntOrNull() ?: 0
}