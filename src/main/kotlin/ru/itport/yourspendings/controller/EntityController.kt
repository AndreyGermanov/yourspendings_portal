package ru.itport.yourspendings.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import javax.persistence.EntityManager
import javax.transaction.Transactional

abstract class EntityController<T>(val entityName:String) {

    @Autowired
    lateinit var entityManager: EntityManager

    @GetMapping("/count")
    open fun count(
        @RequestParam("filter_fields") filterFields:ArrayList<String>?,
        @RequestParam("filter_value") filterValue:String?):Int {
        var sql = "SELECT count(u) FROM $entityName u"
        if (filterFields != null) sql += " WHERE " + filterFields.map {"$it LIKE '$filterValue%'"}.joinToString(" OR ")
        return entityManager.createQuery(sql).resultList[0].toString().toIntOrNull() ?: 0
    }

    @GetMapping("/list")
    open fun list(
            @RequestParam("filter_fields") filterFields:ArrayList<String>?,
            @RequestParam("filter_value") filterValue:String?,
            @RequestParam("limit") limit:Int?,
            @RequestParam("skip") skip:Int?,
            @RequestParam("order") order:String?):Any {
        var sql = "SELECT u FROM $entityName u"
        if (filterFields != null && filterFields.size>0)
            sql += " WHERE " + filterFields.map {"$it LIKE '$filterValue%'"}.joinToString(" OR ")
        if (order != null) sql += " ORDER BY $order"
        val query = entityManager.createQuery(sql)
        if (skip != null && skip>0) query.firstResult = skip
        if (limit != null && limit>0) query.maxResults = limit
        return query.resultList.map { postProcessListItem(it as T) }
    }

    open fun postProcessListItem(item:T):T = item

    @GetMapping("/item/{id}")
    open fun getItem(@PathVariable("id") itemId:String):Any {
        if (itemId === "new") return ""
        val result = entityManager.find(Class.forName("ru.itport.yourspendings.entity.$entityName"),getItemId(itemId))
        return postProcessListItem(result as T) as Any
    }

    @DeleteMapping("/item/{ids}")
    @Transactional
    open fun deleteItem(@PathVariable("ids") ids:String?):HashMap<String,String>? {
        if (ids==null) return null
        entityManager.createQuery("DELETE FROM $entityName WHERE uid IN ($ids)").executeUpdate()
        return hashMapOf("status" to "ok")
    }

    abstract fun getItemId(id:Any):Any

}