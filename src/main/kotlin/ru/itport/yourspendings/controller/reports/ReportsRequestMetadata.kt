package ru.itport.yourspendings.controller.reports

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.ArrayList
import java.util.HashMap
import javax.persistence.EntityManager

data class ReportRequest (
        var query:String = "",
        var parameters: ArrayList<QueryParameter> = ArrayList(),
        var format: ReportRequestOutputFormat
)

data class ReportRequestOutputFormat (
        var title: String = "",
        var columns: List<ReportRequestColumnFormat> = ArrayList(),
        var groups: List<GroupFormat> = ArrayList(),
        var sortOrder: List<SortOrderFieldFormat> = ArrayList(),
        var totals: TotalsFormat = TotalsFormat()
)

data class ReportRequestColumnFormat (
        var id: String = "",
        var title: String,
        var type: String,
        var decimals:Int?,
        var dateFormat:String = "",
        var groupFunction:GroupFunction?
)

enum class GroupFunction {
    min, max, avg, sum, first, last, all
}

data class GroupRow (
        var groupLevel:String,
        var totals: HashMap<Int, HashMap<GroupFunction, Double>> = HashMap()
)

data class GroupFormat (
        var fieldIndex:Int,
        var isHierarchy: Boolean = false,
        var hierarchyIdField: Int = 0,
        var hierarchyNameField:String = "",
        var hierarchyModelName:String = "",
        var hierarchyStartLevel:Int = 0,
        var sortOrder:List<SortOrderFieldFormat> = ArrayList()
)

enum class SortOrderDirection {
    asc,desc
}

data class SortOrderFieldFormat (
        var fieldIndex: Int,
        var direction: SortOrderDirection = SortOrderDirection.asc
)

data class TotalsFormat (
        var display:Boolean = false
)

data class QueryParameter (
        var name:String,
        var value:Any,
        var options:HashMap<String,Any>? = null,
        var entityManager: EntityManager
) {
    fun getParamValue():String {
        if (options != null && (options as HashMap<String,Any>)["hierarchy"]?.toString()?.toBoolean() == true) {
            return getChildrenValuesList().joinToString(",")
        }
        return this.value.toString()
    }

    fun getChildrenValuesList(entity:Any?=null):ArrayList<Int> {
        val result = ArrayList<Int>()
        val options = options as HashMap<String,Any>
        val entityName = "ru.itport.yourspendings.entity."+options["entityName"].toString()
        val idFieldName = options["idFieldName"].toString()
        val parentFieldName = options["parentFieldName"].toString()
        var entity = entity
        if (entity == null) {
            if (this.value is Int) {
                entity = entityManager.find(Class.forName(entityName), this.value.toString().toInt())
            } else if (this.value is ArrayList<*>) {
                (ObjectMapper().readValue(this.value.toString()) as ArrayList<Int>).forEach {
                    entity = entityManager.find(Class.forName(entityName), it)
                    result.addAll(getChildrenValuesList(entity))
                }
                return result
            }
        }
        val idField = entity!!.javaClass.getDeclaredField(idFieldName)
        idField.isAccessible = true
        val id = idField.get(entity).toString().toInt()
        result.add(id)
        val query = "SELECT p FROM ${entityName.split(".").last()} p WHERE $parentFieldName=$id"
        entityManager.createQuery(query).resultList.forEach {
            result.addAll(getChildrenValuesList(it!!))
        }
        return result
    }
}