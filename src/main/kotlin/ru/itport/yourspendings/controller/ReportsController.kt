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
        var currentGroups = ArrayList<HashMap<String,Any>>()
        requests.forEach {request ->
            currentGroups.clear()
            var resultReport = ArrayList<Any>()
            entityManager.createQuery(request.query).apply {
                request.parameters.forEach {
                    this.setParameter(it.key,it.value)
                }
            }.resultList.forEach {row ->
                var resultRow = ArrayList<Any?>()
                var row = row as Array<Any>
                if (request.format.groups.isNotEmpty()) {
                    request.format.groups.forEachIndexed { groupFieldIndex,groupFormat ->
                        var groupFieldValue = groupFormat.fieldIndex
                        if (currentGroups.size - 1 < groupFieldIndex ||
                                currentGroups[groupFieldIndex]["value"].toString() != row[groupFieldValue].toString()) {
                            if (currentGroups.size > groupFieldIndex) {
                                shrinkTo(currentGroups,groupFieldIndex)
                            }
                            var groupRow =  ArrayList<Any>(row.size+1)
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
                    var value = convertFieldValue(request.format.columns[index],row[index])
                    resultRow.add(value)
                    if (column.groupFunction !== null) {
                        currentGroups.forEach {
                            var groupRow = it["row"] as ArrayList<Any>
                            var currentValue = convertFieldValue(request.format.columns[index],groupRow[index])
                            var currentValueDbl = currentValue.toString().toDouble()
                            var valueDbl = value.toString().toDouble()
                            currentValue = when (column.groupFunction) {
                                GroupFunction.avg -> if (currentValueDbl>0) (valueDbl+currentValueDbl)/2 else valueDbl
                                GroupFunction.max -> if (valueDbl>currentValueDbl) valueDbl else currentValue
                                GroupFunction.min -> if (valueDbl<currentValueDbl) valueDbl else currentValue
                                GroupFunction.sum -> currentValueDbl+valueDbl
                                GroupFunction.first -> currentValue ?: valueDbl
                                GroupFunction.last -> valueDbl
                                else -> valueDbl
                            }
                            groupRow[index] = convertFieldValue(request.format.columns[index],
                                    currentValue.toString().toDoubleOrNull() ?: 0.0)!!
                        }
                    }
                }
                resultRow.add(HashMap<String,Any>())
                if (currentGroups.size>0) {
                    var groupRow = currentGroups[currentGroups.size-1]["row"] as ArrayList<Any>
                    (groupRow[groupRow.size-1] as ArrayList<Any>).add(resultRow)
                } else {
                    resultReport.add(resultRow)
                }
            }
            if (request.format.groups.size>0)
                resultReport = processGroup(0,request.format.groups[0],resultReport)
            result.add(resultReport)
        }
        return result
    }

    fun processGroup(level:Int,groupConfig:GroupFormat,items:ArrayList<Any>):ArrayList<Any> {
        if (!groupConfig.isHierarchy) return items
        val result = ArrayList<Any>()
        var groups = HashMap<Int,Any>()
        items.forEach{
            val row = it as ArrayList<Any>
            val parentList = getGroupParentList(row[groupConfig.hierarchyIdField].toString().toInt(),
                    groupConfig.hierarchyModelName,groupConfig.hierarchyNameField)
            parentList.forEach {
                var groupRow = row
                groupRow[groupConfig.fieldIndex] = it["name"].toString()
                groupRow[groupRow.size-1] = ArrayList<Any>()
                if (!groups.containsKey(it["id"].toString().toInt()))
                    groups[it["id"].toString().toInt()] = groupRow
                else
                    groupRow = groups[it["id"].toString().toInt()] as ArrayList<Any>

            }
        }
        return items

    }

    fun getGroupParentList(id:Int,modelName:String,nameField:String):ArrayList<HashMap<String,Any>> {
        val item = entityManager.find(Class.forName("ru.itport.yourspendings.entity.$modelName"),id)
        var field = item.javaClass.getDeclaredField("parent")
        field.isAccessible = true
        val result = ArrayList<HashMap<String,Any>>()
        var parent = field.get(item)
        while (parent != null) {
            var uidField = parent.javaClass.getDeclaredField("uid")
            var textField = parent.javaClass.getDeclaredField(nameField)
            uidField.isAccessible = true
            textField.isAccessible = true
            result.add(hashMapOf("id" to uidField.get(parent),
                    "name" to textField.get(parent)));
            var field = parent.javaClass.getDeclaredField("parent")
            field?.isAccessible = true
            parent = field?.get(parent)
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
                }.also { println(it)}
            }
        }
    }

    fun convertFieldValue(format:ReportRequestColumnFormat,value:Any?):Any? {
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
    var groups: List<GroupFormat> = ArrayList()
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