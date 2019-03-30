package ru.itport.yourspendings.controller.reports

import java.util.ArrayList
import java.util.HashMap

data class ReportRequest (
        var query:String = "",
        var parameters: MutableMap<String,Any> = HashMap(),
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
        var hierarchyModelName:String = ""
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