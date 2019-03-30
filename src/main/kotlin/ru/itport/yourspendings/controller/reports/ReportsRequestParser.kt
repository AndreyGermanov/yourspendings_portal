package ru.itport.yourspendings.controller.reports

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.ArrayList
import java.util.HashMap

class ReportsRequestParser(val body:Any?) {

    var queries: ArrayList<ReportRequest> = ArrayList()

    fun parse() {
        if (body == null) return
        if (body !is ArrayList<*>) return
        val queries = body as ArrayList<MutableMap<String, Any>>
        this.queries = queries.map {query ->
            ReportRequest(
                query = query["query"].toString(),
                parameters = query["parameters"] as? HashMap<String, Any> ?: HashMap(),
                format = this.parseOutputFormat(query["outputFormat"])
            )
        } as? ArrayList<ReportRequest> ?: ArrayList()
    }

    private fun parseOutputFormat(body:Any?):ReportRequestOutputFormat {
        var formatJson = body ?: return ReportRequestOutputFormat()
        if (formatJson is String) formatJson = ObjectMapper().readValue(formatJson)
        return ReportRequestOutputFormat().apply {
            val format = formatJson as? MutableMap<String,Any> ?: return this
            this.title = format["title"].toString()
            this.columns = parseColumns(format["columns"])
            this.groups = parseGroups(format["groups"],this.columns)
            this.sortOrder = parseSortOrder(format["sort"],this.columns)
            this.totals = parseTotals(format["totals"])
        }
    }

    private fun parseColumns(columns:Any?):List<ReportRequestColumnFormat> {
        return (columns as? ArrayList<MutableMap<String, Any>> ?: ArrayList()).map {
            ReportRequestColumnFormat(
                    id = it["id"]?.toString() ?: "",
                    title = it["title"].toString(),
                    type = it["type"].toString(),
                    decimals = it["decimals"].toString().toIntOrNull(),
                    dateFormat = it["dateFormat"]?.toString() ?: "",
                    groupFunction = if (it["groupFunction"] != null && it["groupFunction"].toString().isNotEmpty())
                        GroupFunction.valueOf(it["groupFunction"].toString())
                    else null
            )
        }
    }

    private fun parseGroups(groups:Any?,columns:List<ReportRequestColumnFormat>):List<GroupFormat> {
        return (groups as? ArrayList<HashMap<String, Any>> ?: ArrayList()).filter {
            !it.containsKey("hidden") || !it["hidden"].toString().toBoolean()
        }.map {
            GroupFormat(
                    fieldIndex = getFieldIndex(it["fieldIndex"],columns),
                    isHierarchy = it.containsKey("hierarchy")
            ).apply {
                if (this.isHierarchy) {
                    val options = it["hierarchy"] as HashMap<String, Any>
                    this.hierarchyIdField = getFieldIndex(options["idField"],columns)
                    this.hierarchyNameField = options["nameField"]?.toString() ?: ""
                    this.hierarchyModelName = options["entity"]?.toString() ?: ""
                }
            }
        }
    }

    private fun parseSortOrder(sort:Any?,columns:List<ReportRequestColumnFormat>):List<SortOrderFieldFormat> {
        return (sort as? ArrayList<HashMap<String, Any>> ?: ArrayList()).map {
            SortOrderFieldFormat(
                    fieldIndex = getFieldIndex(it["fieldIndex"],columns),
                    direction = if (it["direction"] != null && it["direction"].toString().isNotEmpty())
                        SortOrderDirection.valueOf(it["direction"].toString())
                    else SortOrderDirection.asc
            )
        }
    }

    private fun parseTotals(totals:Any?):TotalsFormat {
        return TotalsFormat().apply {
            (totals as? HashMap<String, Any> ?: HashMap()).also {
                this.display = it["display"]?.toString()?.toBoolean() ?: false
            }
        }
    }

    private fun getFieldIndex(fieldId:Any?,columns:List<ReportRequestColumnFormat>):Int {
        if (fieldId === null) return 0
        if (fieldId is String) {
            return columns.indexOfFirst { it.id == fieldId }
        } else if (fieldId is Int) return fieldId
        return 0
    }
}