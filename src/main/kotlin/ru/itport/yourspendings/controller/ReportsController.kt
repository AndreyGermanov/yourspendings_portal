@file:Suppress("UNCHECKED_CAST")

package ru.itport.yourspendings.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.entity.Report
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

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

    fun shrinkTo(list: MutableList<*>, newSize: Int)  {
        val size = list.size
        if (newSize >= size) return
        for (i in newSize until size) {
            list.removeAt(list.size - 1)
        }
    }

    @PostMapping("/generate")
    fun generate(@RequestBody body:Any=ArrayList<MutableMap<String,Any>>()):Any? {
        val requests = this.parseReportRequest(body) ?: return null
        val result = ArrayList<Any>()
        val currentGroups = ArrayList<HashMap<String,Any>>()
        requests.forEach {request ->
            currentGroups.clear()
            var resultReport = ArrayList<Any>()
            entityManager.createQuery(request.query).apply {
                request.parameters.forEach {
                    this.setParameter(it.key,it.value)
                }
            }.resultList.forEach {
                val resultRow = ArrayList<Any?>()
                val row = it as Array<Any>
                if (request.format.groups.isNotEmpty()) {
                    request.format.groups.forEachIndexed { groupFieldIndex,groupFormat ->
                        val groupFieldValue = groupFormat.fieldIndex
                        if (currentGroups.size - 1 < groupFieldIndex ||
                                currentGroups[groupFieldIndex]["value"].toString() != row[groupFieldValue].toString()) {
                            if (currentGroups.size > groupFieldIndex) {
                                shrinkTo(currentGroups,groupFieldIndex)
                            }
                            val groupRow =  ArrayList<Any>(row.size+1)
                            row.forEach { groupRow.add("")}
                            groupRow.add("")
                            if (currentGroups.size>0) {
                                val parentGroupRow = currentGroups[currentGroups.size-1]["row"] as ArrayList<Any>
                                (parentGroupRow[parentGroupRow.size-1] as ArrayList<Any>).add(groupRow)
                            } else {
                                resultReport.add(groupRow)
                            }
                            currentGroups.add(hashMapOf("value" to row[groupFieldValue].toString(),
                                    "row" to groupRow))
                            request.format.groups.forEachIndexed { idx, value ->
                                if (idx<=groupFieldIndex) groupRow[idx] = row[value.fieldIndex]
                            }
                            groupRow[row.size] = hashMapOf("groupLevel" to groupFieldIndex, "groupColumn" to groupFieldValue)
                            if (groupFormat.isHierarchy)
                                groupRow[groupFormat.hierarchyIdField] = row[groupFormat.hierarchyIdField]
                            groupRow.add(ArrayList<Any>())
                        }
                    }
                }
                request.format.columns.forEachIndexed {index,column ->
                    val value = convertFieldValue(request.format.columns[index],row[index])
                    resultRow.add(value)
                    if (column.groupFunction !== null) {
                        currentGroups.forEach {
                            val groupRow = it["row"] as ArrayList<Any>
                            var currentValue = convertFieldValue(request.format.columns[index],groupRow[index])
                            val currentValueDbl = currentValue.toString().toDoubleOrNull() ?: 0.0
                            val valueDbl = value.toString().toDouble()
                            currentValue = when (column.groupFunction) {
                                GroupFunction.avg -> {
                                    (currentValue as? ArrayList<Any> ?: ArrayList()).apply { add(valueDbl) }
                                }
                                GroupFunction.max -> if (valueDbl>currentValueDbl) valueDbl else currentValue
                                GroupFunction.min -> if (valueDbl<currentValueDbl) valueDbl else currentValue
                                GroupFunction.sum -> currentValueDbl+valueDbl
                                GroupFunction.first -> currentValue ?: valueDbl
                                GroupFunction.last -> valueDbl
                                else -> valueDbl
                            }
                            groupRow[index] = if (column.groupFunction != GroupFunction.avg) convertFieldValue(request.format.columns[index],
                                    currentValue.toString().toDoubleOrNull() ?: 0.0)!!
                            else currentValue!!
                        }
                    }
                }
                resultRow.add(HashMap<String,Any>())
                if (currentGroups.size>0) {
                    val groupRow = currentGroups[currentGroups.size-1]["row"] as ArrayList<Any>
                    (groupRow[groupRow.size-1] as ArrayList<Any>).add(resultRow)
                } else {
                    resultReport.add(resultRow)
                }
            }
            if (request.format.groups.isNotEmpty()) {
                resultReport = processGroup(0, request.format, resultReport)
                resultReport = applyAggregates(request.format, resultReport)
                if (request.format.sortOrder.size>0) sortList(request.format,resultReport)
                resultReport = toFlatList(request.format,resultReport)
            }
            result.add(resultReport)
        }
        return result
    }

    fun processGroup(level:Int,format:ReportRequestOutputFormat,items:ArrayList<Any>?):ArrayList<Any> {
        val groupConfig = format.groups[level]
        if (items == null || !groupConfig.isHierarchy) return items ?: ArrayList()
        val result = ArrayList<Any>()
        val groups = HashMap<Int,Any>()
        items.forEach {
            val row = it as ArrayList<Any>
            val parentList = getGroupParentList(row[groupConfig.hierarchyIdField].toString().toInt(),
                    groupConfig.hierarchyModelName, groupConfig.hierarchyNameField)
            parentList.add(hashMapOf("id" to row[groupConfig.hierarchyIdField].toString().toInt(),
                    "name" to row[format.groups[level].fieldIndex],
                    "parent" to parentList[parentList.size-1]["id"].toString().toIntOrNull()) as HashMap<String,Any>)
            var groupRow = ArrayList<Any>()
            parentList.forEach {
                val parent = groups[it["parent"]?.toString()?.toIntOrNull() ?: 0] as? HashMap<String,Any>
                if (!groups.containsKey(it["id"].toString().toInt())) {
                    groupRow = ArrayList()
                    format.columns.forEach { groupRow.add("") }
                    if (parent != null) {
                        format.groups.forEach {
                            groupRow[it.fieldIndex] = (parent["data"] as ArrayList<Any>)[it.fieldIndex]
                        }
                        ((parent["data"] as ArrayList<Any>)[format.columns.size + 1] as ArrayList<Any>).add(groupRow)
                    } else {
                        result.add(groupRow)
                    }
                    groupRow[groupConfig.fieldIndex] = it["name"].toString()
                    groupRow.add(row[format.columns.size])
                    groupRow.add(ArrayList<Any>())
                    groups[it["id"].toString().toInt()] = hashMapOf("parent" to parent, "data" to groupRow)
                } else {
                    groupRow = (groups[it["id"].toString().toInt()] as HashMap<String,Any>)["data"] as ArrayList<Any>
                }
                format.columns.forEachIndexed {index,column ->
                    if (column.groupFunction != null) {
                        val value = convertFieldValue(format.columns[index],row[index])
                        var currentValue = convertFieldValue(format.columns[index],groupRow[index])
                        val currentValueDbl = currentValue.toString().toDoubleOrNull() ?: 0.0
                        val valueDbl = value.toString().toDoubleOrNull() ?: 0.0
                        currentValue = when (column.groupFunction) {
                            GroupFunction.avg -> {
                                (currentValue as? ArrayList<Any> ?: ArrayList()).apply { add(calcAverage(value as ArrayList<Any>)) }
                            }
                            GroupFunction.max -> if (valueDbl>currentValueDbl) valueDbl else currentValue
                            GroupFunction.min -> if (valueDbl<currentValueDbl) valueDbl else currentValue
                            GroupFunction.sum -> currentValueDbl+valueDbl
                            GroupFunction.first -> currentValue ?: valueDbl
                            GroupFunction.last -> valueDbl
                            else -> valueDbl
                        }
                        groupRow[index] = if (column.groupFunction != GroupFunction.avg) convertFieldValue(format.columns[index],
                                currentValue.toString().toDoubleOrNull() ?: 0.0)!!
                        else currentValue!!
                    }
                }
            }
            row[format.columns.size + 1] = processGroup(level+1,format,row[format.columns.size + 1] as? ArrayList<Any>)
            (groupRow[format.columns.size + 1] as ArrayList<Any>).addAll(row[format.columns.size + 1] as ArrayList<Any>)
        }
        return result
    }

    fun applyAggregates(format:ReportRequestOutputFormat,items:ArrayList<Any>?):ArrayList<Any> {
        val items = items ?: return ArrayList()
        return items.map {item ->
            val item = item as ArrayList<Any>
            format.columns.forEachIndexed { index,column ->
                if (column.groupFunction == GroupFunction.avg && (item[format.columns.size] as HashMap<String,Any>).containsKey("groupLevel")) {
                    var list = (item[index] as? ArrayList<Any> ?: ArrayList())

                    var value = 0.0
                    if (list.size>0)
                        value = calcAverage(list)

                    item[index] = convertFieldValue(column,value)!!
                }
            }
            if (item.size>format.columns.size+1) {
                item[format.columns.size+1] = applyAggregates(format, item[format.columns.size+1] as? ArrayList<Any>)
            }
            item
        } as ArrayList<Any>
    }

    fun toFlatList(format:ReportRequestOutputFormat,items:ArrayList<Any>):ArrayList<Any> {
        val result = ArrayList<Any>()
        items.forEach {
            val item = it as ArrayList<Any>
            result.add(ArrayList<Any>().apply {
                item.forEachIndexed{ index,it -> if (index<item.size+1) add(it) }
            })
            var nested:ArrayList<Any>? = null
            if (item.size>format.columns.size+1) {
                nested = item[format.columns.size + 1] as? ArrayList<Any>
                if (nested != null && nested.size > 0) result.addAll(toFlatList(format, nested))
            }
        }
        return result
    }

    fun sortList(format:ReportRequestOutputFormat,items:ArrayList<Any>) {

        items.sortWith(Comparator {item1,item2 ->
            var value = 0
            format.sortOrder.map {
                when (it.direction) {
                    SortOrderDirection.asc ->
                ((item1 as ArrayList<Any>)[it.fieldIndex].toString().toDouble()-
                        (item2 as ArrayList<Any>)[it.fieldIndex].toString().toDouble()).roundToInt()
                    SortOrderDirection.desc ->
                        ((item2 as ArrayList<Any>)[it.fieldIndex].toString().toDouble()-
                        (item1 as ArrayList<Any>)[it.fieldIndex].toString().toDouble()).roundToInt()
                }
            }.reduce { acc, i ->
                if (acc==0) i; else acc
            }
        })
        items.forEach {
            if ((it as ArrayList<Any>).size>format.columns.size+1) {
                sortList(format, it[format.columns.size + 1] as ArrayList<Any>)
            }
        }
    }

    fun calcAverage(list:ArrayList<Any>):Double {
        return list.map {it.toString().toDoubleOrNull() ?: 0.0}.reduce { acc, d -> acc+d } / list.size
    }

    fun getGroupParentList(id:Int,modelName:String,nameField:String):ArrayList<HashMap<String,Any>> {
        val item = entityManager.find(Class.forName("ru.itport.yourspendings.entity.$modelName"),id)
        val field = item.javaClass.getDeclaredField("parent")
        field.isAccessible = true
        val result = ArrayList<HashMap<String,Any>>()
        var parent = field.get(item)
        while (parent != null) {
            var uidField = parent.javaClass.getDeclaredField("uid")
            var textField = parent.javaClass.getDeclaredField(nameField)
            uidField.isAccessible = true
            textField.isAccessible = true
            var field = parent.javaClass.getDeclaredField("parent")
            field?.isAccessible = true
            var resultRow = hashMapOf("id" to uidField.get(parent), "name" to textField.get(parent))
            parent = field?.get(parent)
            if (parent != null) {
                var parentUidField = parent.javaClass.getDeclaredField("uid")
                parentUidField.isAccessible = true
                resultRow["parent"] = parentUidField.get(parent)
            }
            result.add(resultRow)
        }
        result.reverse()
        return result
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
        var formatJson = formatJson ?: return ReportRequestOutputFormat()
        if (formatJson is String) {
            formatJson = ObjectMapper().readValue(formatJson)
        }
        return ReportRequestOutputFormat().apply {
            val format = formatJson as? MutableMap<String,Any> ?: return this
            this.title = format["title"].toString()
            this.columns = (format["columns"] as? ArrayList<MutableMap<String,Any>>)?.map {
                ReportRequestColumnFormat(
                        title = it["title"].toString(),
                        type = it["type"].toString(),
                        decimals = it["decimals"].toString().toIntOrNull(),
                        dateFormat = it["dateFormat"]?.toString() ?: "",
                        groupFunction = if (it["groupFunction"] != null && it["groupFunction"].toString().isNotEmpty())
                                            GroupFunction.valueOf(it["groupFunction"].toString())
                                        else null
                )
            } ?: ArrayList()
            this.groups = (format["groups"] as? ArrayList<HashMap<String,Any>> ?: ArrayList()).map {
                GroupFormat(
                    fieldIndex = it["fieldIndex"].toString().toIntOrNull() ?: 0,
                    isHierarchy = it.containsKey("hierarchy")
                ).apply {
                    if (this.isHierarchy) {
                        val options = it["hierarchy"] as HashMap<String,Any>
                        this.hierarchyIdField = options["idField"].toString().toIntOrNull() ?: 0
                        this.hierarchyNameField = options["nameField"]?.toString() ?: ""
                        this.hierarchyModelName = options["entity"]?.toString() ?: ""
                    }
                }
            }
            this.sortOrder = (format["sort"] as? ArrayList<HashMap<String,Any>> ?: ArrayList()).map {
                SortOrderFieldFormat(
                        fieldIndex = it["fieldIndex"].toString().toIntOrNull() ?: 0,
                        direction = if (it["direction"] != null && it["direction"].toString().isNotEmpty())
                            SortOrderDirection.valueOf(it["direction"].toString())
                        else SortOrderDirection.asc
                )
            }
        }
    }

    fun convertFieldValue(format:ReportRequestColumnFormat,value:Any?):Any? {
        if (value is ArrayList<*>) return value
        return when (format.type) {
            "decimal" -> convertDecimalValue(format,value)
            "integer" -> value.toString().toIntOrNull() ?: 0
            "date","datetime","time","timestamp" -> convertDateTimeValue(format,value)
            else -> value
        }
    }

    fun convertDecimalValue(format:ReportRequestColumnFormat,value:Any?):Double {
        var value = value.toString().toDoubleOrNull() ?: return 0.0
        if (format.decimals != null)
            value = BigDecimal(value).setScale(format.decimals!!, RoundingMode.HALF_EVEN).toDouble()
        return value
    }

    fun convertDateTimeValue(format:ReportRequestColumnFormat,value:Any?):String {
        var dt = value as? Date ?: return ""
        var date = LocalDateTime.ofEpochSecond(dt.time/1000,0, ZoneOffset.UTC)
        if (format.type == "timestamp")
            return date.toInstant(ZoneOffset.UTC).epochSecond.toString()
        val format = when {
            format.dateFormat.isNotEmpty() -> format.dateFormat
            format.type == "date" -> "YYYY-MM-DD"
            format.type == "datetime" -> "YYYY-MM-DD HH:mm:ss"
            format.type == "time" -> "HH:mm:ss"
            else -> "YYYY-MM-DD HH:mm:ss"
        }
        return date.format(DateTimeFormatter.ofPattern(format))
    }
}

data class ReportRequest (
    var query:String = "",
    var parameters: MutableMap<String,Any> = HashMap(),
    var format: ReportRequestOutputFormat
)

data class ReportRequestOutputFormat (
    var title: String = "",
    var columns: List<ReportRequestColumnFormat> = ArrayList(),
    var groups: List<GroupFormat> = ArrayList(),
    var sortOrder: List<SortOrderFieldFormat> = ArrayList()
)

data class ReportRequestColumnFormat (
    var title: String,
    var type: String,
    var decimals:Int?,
    var dateFormat:String = "",
    var groupFunction:GroupFunction?
)

enum class GroupFunction {
    min, max, avg, sum, first, last
}

data class GroupRow (
    var groupLevel:String,
    var totals: HashMap<Int,HashMap<GroupFunction,Double>> = HashMap()
)

data class GroupFormat (
    var fieldIndex:Int,
    var isHierarchy: Boolean = false,
    var hierarchyIdField: Int = 0,
    var hierarchyNameField:String = "",
    var hierarchyModelName:String = ""
)

enum class SortOrderDirection {
    asc,desc
}

data class SortOrderFieldFormat (
    var fieldIndex: Int,
    var direction: SortOrderDirection = SortOrderDirection.asc
)