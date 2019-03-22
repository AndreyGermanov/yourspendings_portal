package ru.itport.yourspendings.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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

    @PostMapping("/generate")
    fun generate(@RequestBody body:Any=ArrayList<MutableMap<String,Any>>()):Any? {
        val requests = this.parseReportRequest(body) ?: return null
        return requests.map {
            entityManager.createQuery(it.query).apply {
                it.parameters.forEach {
                    this.setParameter(it.key,it.value)
                }
            }.resultList.map {
                it
            }
        }
    }

    fun parseReportRequest(body:Any?):ArrayList<ReportRequest>? {
        if (body == null) return null
        if (body !is ArrayList<*>) return null
        val queries = body as ArrayList<MutableMap<String,Any>>
        return queries.map {query ->
            ReportRequest(
                query = query["query"].toString(),
                parameters = query["parameters"] as? HashMap<String,Any> ?: HashMap(),
                format = this.parseOutputFormat(query["outputFormat"])
            )
        } as? ArrayList<ReportRequest> ?: ArrayList()
    }

    fun parseOutputFormat(formatJson:Any?):ReportRequestOutputFormat {
        return ReportRequestOutputFormat().apply {
            val format = formatJson as? HashMap<String,Any> ?: return this
            this.title = format["title"].toString()
            this.columns = (format["columns"] as? ArrayList<MutableMap<String,Any>>)?.map {
                ReportRequestColumnFormat(title = it["title"].toString(),type = it["type"].toString())
            } ?: ArrayList()
        }
    }

}

data class ReportRequest (
    var query:String = "",
    var parameters: MutableMap<String,Any> = HashMap(),
    var format: ReportRequestOutputFormat
)

data class ReportRequestOutputFormat (
    var title: String = "",
    var columns: List<ReportRequestColumnFormat> = ArrayList()
)

data class ReportRequestColumnFormat (
    var title: String,
    var type: String
)

