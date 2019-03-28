package ru.itport.yourspendings.controller.reports

import java.util.*
import javax.persistence.EntityManager

class ReportsBuilder(val entityManager: EntityManager, val requests: ArrayList<ReportRequest>) {

    var reports: ArrayList<Any> = ArrayList()

    fun build() {
        reports = requests.map { request ->
            ReportBuilder(entityManager,request).build() as Any
        } as ArrayList<Any>
    }



}