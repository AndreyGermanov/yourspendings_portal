package ru.itport.yourspendings.controller.reports

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import javax.persistence.EntityManager

class ReportBuilder(val entityManager: EntityManager, val request: ReportRequest) {

    val currentGroups = ArrayList<HashMap<String, Any>>()
    var resultReport = ArrayList<Any>()

    fun build():ArrayList<Any> {
        var data = requestDataFromDB(request.query,request.parameters) as ArrayList<Any>
        data = parseData(data)
        val sortOrder = request.format.groups.map {
            SortOrderFieldFormat(fieldIndex = it.fieldIndex,direction = SortOrderDirection.asc)
        }
        sortList(sortOrder,data,false)
        groupData(data)
        processGroups()
        processTotals()
        resultReport = toFlatList(request.format,resultReport)
        return resultReport
    }

    fun requestDataFromDB(query:String,parameters:ArrayList<QueryParameter>):MutableList<Any?> {
        var query = query
        parameters.forEach {
            query = query.replace(":${it.name}",it.getParamValue())
        }
        return entityManager.createQuery(query).resultList
    }

    fun parseData(items:ArrayList<Any>):ArrayList<Any> {
        return items.map {
            val row = it as Array<Any>
            val resultRow = ArrayList<Any?>()
            row.forEachIndexed { index, data->
                var value = data
                if (request.format.columns.size>index) {
                    value = convertFieldValue(request.format.columns[index],data)!!
                }
                resultRow.add(value)
            }
            resultRow
        } as ArrayList<Any>
    }

    fun groupData(data:ArrayList<Any>) {
        data.forEach {
            val row = it as ArrayList<Any>
            extractGroupsFromRow(row)
            val resultRow = ArrayList<Any?>()
            row.forEachIndexed { index, value ->
                val value = row[index]
                if (request.format.columns.isNotEmpty() && index < request.format.columns.size) {
                    val column = request.format.columns[index]
                    if (column.groupFunction !== null) {
                        currentGroups.forEach {
                            val groupRow = it["row"] as ArrayList<Any>
                            groupRow[index] = applyAggregateToColumn(request.format,groupRow,index,value)
                        }
                    }
                }
                resultRow.add(value)
            }
            resultRow.add(HashMap<String,Any>())
            if (currentGroups.size>0) {
                val groupRow = currentGroups[currentGroups.size-1]["row"] as ArrayList<Any>
                (groupRow[groupRow.size-1] as ArrayList<Any>).add(resultRow)
            } else {
                resultReport.add(resultRow)
            }
        }
    }

    fun extractGroupsFromRow(row:ArrayList<Any>) {
        if (request.format.groups.isEmpty()) return
        request.format.groups.forEachIndexed { groupFieldIndex,groupFormat ->
            val groupFieldValue = groupFormat.fieldIndex
            if (currentGroups.size - 1 < groupFieldIndex ||
                    currentGroups[groupFieldIndex]["value"].toString() != row[groupFieldValue].toString()) {
                if (currentGroups.size > groupFieldIndex) {
                    shrinkTo(currentGroups, groupFieldIndex)
                }
                val groupRow = ArrayList<Any>(row.size + 1)
                row.forEach { groupRow.add("") }
                groupRow.add("")
                if (currentGroups.size > 0) {
                    val parentGroupRow = currentGroups[currentGroups.size - 1]["row"] as ArrayList<Any>
                    (parentGroupRow[parentGroupRow.size - 1] as ArrayList<Any>).add(groupRow)
                } else {
                    resultReport.add(groupRow)
                }
                currentGroups.add(hashMapOf("value" to row[groupFieldIndex].toString(),
                        "row" to groupRow))
                request.format.groups.forEachIndexed { idx, value ->
                    if (idx < groupFieldIndex) groupRow[idx] = row[value.fieldIndex].toString()
                }
                groupRow[groupFieldIndex] = row[groupFieldIndex]
                groupRow[row.size] = hashMapOf("groupLevel" to groupFieldIndex, "groupColumn" to groupFieldValue)
                if (groupFormat.isHierarchy) groupRow[groupFormat.hierarchyIdField] = row[groupFormat.hierarchyIdField]
                groupRow.add(ArrayList<Any>())
            }
        }
    }

    fun processGroups() {
        if (request.format.groups.isEmpty()) return
        resultReport = processGroup(0, request.format, resultReport)
        resultReport = applyAggregates(request.format, resultReport)
        if (request.format.sortOrder.isNotEmpty()) sortList(request.format.sortOrder,resultReport,true)
    }

    fun processTotals() {
        if (!request.format.totals.display) return
        var totalsRow = ArrayList<Any>().apply {
            request.format.columns.forEach {
                add("")
            }
            add(hashMapOf("totalsRow" to true,"groupLevel" to 0))
        }
        resultReport.forEach {
            val row = it as ArrayList<Any>
            totalsRow = applyAggregatesToRow(request.format,totalsRow,row)
        }
        totalsRow = applyAggregates(request.format,arrayListOf(totalsRow)).first() as ArrayList<Any>
        resultReport.add(totalsRow)
    }


    fun applyAggregateToColumn(format: ReportRequestOutputFormat, groupRow: ArrayList<Any>, index:Int, value:Any):Any {
        val column = format.columns[index]
        if (column.groupFunction == null) return groupRow[index]
        val value = value
        var currentValue = groupRow[index]
        val currentValueDbl = currentValue.toString().toDoubleOrNull() ?: 0.0
        val valueDbl = value.toString().toDoubleOrNull() ?: 0.0
        currentValue = when (column.groupFunction) {
            GroupFunction.avg -> {
                (currentValue as? ArrayList<Any> ?: ArrayList()).apply {
                    if (value is ArrayList<*>)
                        add(calcAverage(value as ArrayList<Any>))
                    else
                        add(value)
                }
            }
            GroupFunction.all ->
                (currentValue as? TreeSet<Any> ?: TreeSet()).apply {
                    if (value is TreeSet<*>)
                        addAll(value)
                    else
                        addAll(value.toString().split(","))
                }
            GroupFunction.max -> if (valueDbl>currentValueDbl) valueDbl else currentValue
            GroupFunction.min -> if (valueDbl<currentValueDbl) valueDbl else currentValue
            GroupFunction.sum -> currentValueDbl+valueDbl
            GroupFunction.first -> currentValue ?: valueDbl
            GroupFunction.last -> value
            else -> valueDbl
        }
        groupRow[index] = currentValue

        return groupRow[index]
    }

    fun processGroup(level:Int,format:ReportRequestOutputFormat,items: ArrayList<Any>?): ArrayList<Any> {
        val groupConfig = format.groups[level]
        if (items == null) return items ?: ArrayList()
        val result = ArrayList<Any>()
        val groups = HashMap<Int,Any>()
        items.forEach {
            var groupRow = ArrayList<Any>()
            val row = it as ArrayList<Any>
            if (groupConfig.isHierarchy) {
                var parentList = ArrayList<HashMap<String, Any>>()
                parentList = getGroupParentList(row[groupConfig.hierarchyIdField].toString().toInt(),
                        groupConfig.hierarchyModelName, groupConfig.hierarchyNameField)
                if (groupConfig.hierarchyStartLevel>0) {
                    for (i in 0 until groupConfig.hierarchyStartLevel) {
                        parentList.removeAt(0)
                    }
                }
                parentList.add(hashMapOf("id" to row[groupConfig.hierarchyIdField].toString().toInt(),
                        "name" to row[format.groups[level].fieldIndex],
                        "parent" to if (parentList.isNotEmpty()) parentList[parentList.size - 1]["id"].toString().toIntOrNull() else null) as HashMap<String,Any>)
                parentList.forEachIndexed { hIndex, it ->
                    val parent = groups[it["parent"]?.toString()?.toIntOrNull() ?: 0] as? HashMap<String, Any>
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
                        groupRow.add((row[format.columns.size] as HashMap<String, Any>).clone())
                        groupRow.add(ArrayList<Any>())
                        groups[it["id"].toString().toInt()] = hashMapOf("parent" to parent, "data" to groupRow)
                    } else {
                        groupRow = (groups[it["id"].toString().toInt()] as HashMap<String, Any>)["data"] as ArrayList<Any>
                    }
                    groupRow = applyAggregatesToRow(format, groupRow, row)
                    (groupRow[format.columns.size] as HashMap<String, Any>)["hierarchyLevel"] = hIndex
                }
            }
            if (level < format.groups.size-1) {
                row[format.columns.size + 1] = processGroup(level + 1, format, row[format.columns.size + 1] as? ArrayList<Any>)
            }
            if (!groupConfig.isHierarchy) result.add(row)
            if (groupRow.isNotEmpty())
                (groupRow[format.columns.size + 1] as ArrayList<Any>).addAll(row[format.columns.size + 1] as ArrayList<Any>)
        }
        return result
    }

    fun applyAggregatesToRow(format:ReportRequestOutputFormat, groupRow: ArrayList<Any>, row: ArrayList<Any>): ArrayList<Any> {
        format.columns.forEachIndexed {index,column ->
            groupRow[index] = applyAggregateToColumn(format,groupRow,index,row[index])
        }
        return groupRow
    }

    fun applyAggregates(format:ReportRequestOutputFormat,items: ArrayList<Any>?): ArrayList<Any> {
        val items = items ?: return ArrayList()
        return items.map {item ->
            val item = item as ArrayList<Any>
            format.columns.forEachIndexed { index,column ->
                if  ((item[format.columns.size] as HashMap<String, Any>).containsKey("groupLevel")) {
                    if (column.groupFunction == GroupFunction.avg) {
                        var list = (item[index] as? ArrayList<Any> ?: ArrayList())

                        var value = 0.0
                        if (list.size > 0)
                            value = calcAverage(list)

                        item[index] = convertFieldValue(column, value)!!
                    } else if (column.groupFunction == GroupFunction.all) {
                        var list = (item[index] as? TreeSet<Any> ?: TreeSet())
                        if (list.size > 0) item[index] = list.joinToString(",")
                    } else if (column.groupFunction != null) {
                        item[index] = convertFieldValue(column, item[index])!!
                    }
                }
            }
            if (item.size>format.columns.size+1) {
                item[format.columns.size+1] = applyAggregates(format, item[format.columns.size+1] as? ArrayList<Any>)
            }
            item
        } as ArrayList<Any>
    }

    fun toFlatList(format:ReportRequestOutputFormat, items: ArrayList<Any>, parentRowNumber:Int?=null): ArrayList<Any> {
        val result = ArrayList<Any>()
        var startingRow = parentRowNumber ?: 0
        items.forEach {
            val item = it as ArrayList<Any>
            result.add(ArrayList<Any>().apply {
                item.forEachIndexed{ index,it ->
                    if (format.columns.isEmpty() || index<format.columns.size + 1) { add(it) }
                }
                if (parentRowNumber!=null) {
                    (this[format.columns.size] as? HashMap<String, Any> ?: HashMap())["parent"] = parentRowNumber-1
                }
            })
            startingRow ++
            var nested: ArrayList<Any>?
            if (format.columns.isNotEmpty() && item.size>format.columns.size+1) {
                nested = item[format.columns.size + 1] as? ArrayList<Any>
                if (nested != null && nested.size > 0) {
                    val nestedList = toFlatList(format, nested,startingRow)
                    result.addAll(nestedList)
                    startingRow += nestedList.size
                }
            }
        }
        return result
    }

    fun sortList(format:List<SortOrderFieldFormat>,items: ArrayList<Any>,useGroupConfig:Boolean) {
        if (format.isEmpty()) return
        items.sortWith(Comparator {item1,item2 ->
            format.map {
                when (it.direction) {
                    SortOrderDirection.asc ->
                        compareFieldValues(request.format.columns[it.fieldIndex],
                                (item1 as ArrayList<Any>)[it.fieldIndex],
                                (item2 as ArrayList<Any>)[it.fieldIndex])
                    SortOrderDirection.desc ->
                        compareFieldValues(request.format.columns[it.fieldIndex],
                                (item1 as ArrayList<Any>)[it.fieldIndex],
                                (item2 as ArrayList<Any>)[it.fieldIndex])*-1
                }
            }.reduce { acc, i ->
                if (acc==0) i; else acc
            }
        })
        items.forEach {
            if ((it as ArrayList<Any>).size>request.format.columns.size+1) {
                var format = format
                if (useGroupConfig) {
                    val groupColumn = (it[request.format.columns.size] as? HashMap<String,Any> ?: HashMap())["groupColumn"] as? Int
                    if (groupColumn != null && request.format.groups.size >= groupColumn) {
                        val sortOrder = request.format.groups[groupColumn].sortOrder
                        if (sortOrder.isNotEmpty()) format = sortOrder
                    }
                }
                sortList(format, it[request.format.columns.size + 1] as ArrayList<Any>,true)
            }
        }
    }

    fun calcAverage(list: ArrayList<Any>):Double {
        return list.map {it.toString().toDoubleOrNull() ?: 0.0}.reduce { acc, d -> acc+d } / list.size
    }

    fun getGroupParentList(id:Int,modelName:String,nameField:String): ArrayList<HashMap<String, Any>> {
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

    fun convertFieldValue(format:ReportRequestColumnFormat,value:Any?):Any? {
        if (value is ArrayList<*>) return value
        return when (format.type) {
            "decimal" -> convertDecimalValue(format,value)
            "integer" -> value.toString().toIntOrNull() ?: value.toString()
            "date","datetime","time","timestamp","weekday", "weekday_number" -> convertDateTimeValue(format,value)
            else -> value
        }
    }

    fun compareFieldValues(format:ReportRequestColumnFormat,item1:Any,item2:Any):Int {
        return when (format.type) {
            "decimal" -> (item1.toString().toDouble().compareTo(item2.toString().toDouble()))
            "integer" -> (item1.toString().toInt().compareTo(item2.toString().toInt()))
            "timestamp" -> (item1.toString().toLong().compareTo(item2.toString().toLong()))
            else -> item1.toString().compareTo(item2.toString())
        }
    }

    fun convertDecimalValue(format:ReportRequestColumnFormat,value:Any?):Double {
        var value = value.toString().toDoubleOrNull() ?: return value as? Double ?: 0.0
        if (format.decimals != null)
            value = BigDecimal(value).setScale(format.decimals!!, RoundingMode.HALF_EVEN).toDouble()
        return value
    }

    fun convertDateTimeValue(format:ReportRequestColumnFormat,value:Any?):String {
        var dt = value as? Date ?: return value.toString()
        var date = LocalDateTime.ofEpochSecond(dt.time/1000,0, ZoneOffset.UTC)
        if (format.type == "timestamp")
            return date.toInstant(ZoneOffset.UTC).epochSecond.toString()
        if (format.type == "weekday")
            return date.dayOfWeek.name
        if (format.type == "weekday_number")
            return date.dayOfWeek.value.toString()
        val format = when {
            format.dateFormat.isNotEmpty() -> format.dateFormat
            format.type == "date" -> "YYYY-MM-dd"
            format.type == "datetime" -> "YYYY-MM-dd HH:mm:ss"
            format.type == "time" -> "HH:mm:ss"
            else -> "YYYY-MM-dd HH:mm:ss"
        }
        return date.format(DateTimeFormatter.ofPattern(format))
    }

    fun shrinkTo(list: MutableList<*>, newSize: Int)  {
        val size = list.size
        if (newSize >= size) return
        for (i in newSize until size) {
            list.removeAt(list.size - 1)
        }
    }
}