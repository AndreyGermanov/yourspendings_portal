package ru.itport.yourspendings.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.lang.reflect.Field
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.OneToMany
import javax.transaction.Transactional

@Suppress("UNCHECKED_CAST")
abstract class EntityController<T>(val entityName:String) {

    @Autowired
    lateinit var entityManager: EntityManager

    @PostMapping("/count")
    open fun count(@RequestBody body:Any = HashMap<String,Any>()):Int = entityManager.createQuery(
            StringBuilder("SELECT count(u) FROM $entityName u").apply {
                parseListRequest(body).also {this.append(createWhereClause(it))}
            }.toString()
        ).resultList[0].toString().toIntOrNull() ?: 0

    @PostMapping("/list")
    open fun list(@RequestBody body:Any? = HashMap<String,Any>()):Any {
        val req = parseListRequest(body)
        return entityManager.createQuery(
            StringBuilder("SELECT u FROM $entityName u").apply {
                this.append("${createWhereClause(req)} ${createOrderClause(req)}")
            }.toString()
        ).apply {
            if (req.skip != null && req.skip!!>0) this.firstResult = req.skip!!
            if (req.limit != null && req.limit!!>0) this.maxResults = req.limit!!
        }.resultList.map { postProcessListItem(it as T) }
    }

    open fun postProcessListItem(item:T):Any = item as Any

    @GetMapping("/item/{id}")
    open fun getItem(@PathVariable("id") itemId:String):Any {
        if (itemId === "new") return ""
        val result = entityManager.find(Class.forName("ru.itport.yourspendings.entity.$entityName"),getItemId(itemId))
        return postProcessListItem(result as T)
    }

    @PostMapping("/item")
    @Transactional
    open fun postItem(@RequestBody body:Any? = HashMap<String,Any>()): Any {
        val fields = body as? HashMap<String,Any> ?:
        return hashMapOf("status" to "error","errors" to hashMapOf("general" to "Incorrect input data"))
        val entity = createModel("ru.itport.yourspendings.entity.$entityName",fields)
        entityManager.persist(entity)
        return hashMapOf("status" to "ok", "result" to this.postProcessListItem(entity as T))
    }

    @DeleteMapping("/item/{ids}")
    @Transactional
    open fun deleteItem(@PathVariable("ids") ids:String?):HashMap<String,String>? {
        if (ids==null) return null
        entityManager.createQuery("DELETE FROM $entityName WHERE uid IN ($ids)").executeUpdate()
        return hashMapOf("status" to "ok")
    }


    private fun createModel(entityName:String,fields:HashMap<String,Any>):Any {
        val entity = if (fields.containsKey("uid")) {
            entityManager.find(Class.forName(entityName),fields["uid"]) ?: Class.forName(entityName).newInstance()
        } else Class.forName(entityName).newInstance()
        Class.forName(entityName).declaredFields.forEach {
            if (fields.containsKey(it.name) && fields[it.name] != null) {
                it.isAccessible = true
                val value = getFieldValue(it,fields[it.name]!!,it.type,entity)
                if (value !== null) it.set(entity,value)
            }
        }
        return entity
    }

    private fun getFieldValue(field:Field,value:Any,type:Class<*>,entity:Any):Any? {
        return getRelationFieldValue(field,value,entity) ?: when (type.name) {
            "java.lang.String" -> value.toString()
            "java.lang.Integer" -> value.toString().toIntOrNull() ?: 0
            "java.lang.Long" -> value.toString().toLongOrNull() ?: 0
            "double" -> value.toString().toDoubleOrNull() ?: 0
            "boolean" -> value.toString() == "1" || value.toString().toBoolean()
            "java.util.Date" -> Date(LocalDateTime.parse(value.toString()).toInstant(ZoneOffset.UTC).epochSecond*1000)
            else -> null
        }
    }

    private fun getRelationFieldValue(field:Field,value:Any,entity:Any):Any? {
        val relationType = getRelationType(field) ?: return null
        return if (relationType.annotationClass.simpleName == "OneToOne" ||
                relationType.annotationClass.simpleName == "ManyToOne") {
            entityManager.find(Class.forName(field.type.canonicalName), value)
        } else if (relationType.annotationClass.simpleName == "OneToMany") {
            val typeName = field.genericType.typeName.split("<").last().split(">").first()
            (entity.javaClass.getDeclaredField(field.name).apply { isAccessible = true }.get(entity) as? List<*>)?.let {

                it.forEach { entityManager.remove(it) }
            }
            (value as List<*>).map {
                this.createModel(typeName, it as HashMap<String, Any>).apply {
                    Class.forName(typeName).getDeclaredField((relationType as OneToMany).mappedBy).also {
                        it.isAccessible = true
                        it.set(this, entity)
                    }
                }
            }
        } else if (relationType.annotationClass.simpleName == "JoinTable") {
            val typeName = field.genericType.typeName.split("<").last().split(">").first()
            (value as List<*>).map {
                val relation = it as HashMap<Any, Any>
                entityManager.find(Class.forName(typeName), relation.values.first()
                        .toString().toIntOrNull()
                        ?: relation.values.first())
            }
        } else {
            null
        }
    }

    private fun getRelationType(it: Field):Annotation? {
        return it.declaredAnnotations.firstOrNull {
            listOf("OneToMany","JoinTable","OneToOne","ManyToOne").contains(it.annotationClass.simpleName)
        }
    }

    abstract fun getItemId(id:Any):Any

    fun parseListRequest(body:Any?):ListRequest {
        if (body is ListRequest) return body
        return (body as? HashMap<String,Any>)?.let {
            ListRequest().apply {
                filterFields = it["filter_fields"].toString().split(",") as? ArrayList<String> ?: ArrayList()
                filterValue = it["filter_value"]?.toString() ?: ""
                condition = it["condition"]?.toString() ?: ""
                limit = it["limit"]?.toString()?.toIntOrNull() ?: 0
                skip = it["skip"]?.toString()?.toIntOrNull() ?: 0
                order = it["order"]?.toString()
            }
        } ?: ListRequest()
    }

    private fun createWhereClause(req:ListRequest):String =
        StringBuilder("").apply {
            var conditionClause = ""
            if (req.condition !== null && req.condition!!.isNotEmpty()) conditionClause = req.condition!!
            if (req.filterFields != null && req.filterFields!!.size>0) {
                if (conditionClause.isNotEmpty()) conditionClause += " AND "
                req.filterFields?.let {
                    if (it.isNotEmpty())
                        conditionClause += "("+ it.joinToString(" OR ") {
                            "${getFieldPresentationForList(it)} LIKE '${req.filterValue}%'"} +")"
                }
            }
            if (conditionClause.isNotEmpty()) this.append(" WHERE $conditionClause")
        }.toString()

    private fun createOrderClause(req:ListRequest):String {
        if (req.order == null || req.order!!.isEmpty() || req.order!!.split(" ").size != 2) return ""
        val parts = req.order!!.split(" ")
        return " ORDER BY ${getFieldPresentationForList(parts[0])} ${parts[1]}"
    }

    open fun getFieldPresentationForList(fieldName:String) = fieldName
}

data class ListRequest(
        var filterFields:ArrayList<String>?=ArrayList(),
        var filterValue:String?="",
        var condition:String?="",
        var limit:Int?=0,
        var skip:Int?=0,
        var order:String?=""
)