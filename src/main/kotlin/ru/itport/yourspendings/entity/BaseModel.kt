package ru.itport.yourspendings.entity

import java.lang.reflect.Field
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.OneToMany

open  class BaseModel {
    companion object {
        lateinit var entityManager:EntityManager

        fun setup(entityManager:EntityManager) {
            this.entityManager = entityManager
        }

        fun createModel(entityName:String,fields: HashMap<String, Any>):Any {
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

        private fun getFieldValue(field: Field, value:Any, type:Class<*>, entity:Any):Any? {
            return getRelationFieldValue(field,value,entity) ?: when (type.name) {
                "java.lang.String" -> value.toString()
                "java.lang.Integer" -> value.toString().toIntOrNull() ?: 0
                "int" -> value.toString().toIntOrNull() ?: 0
                "java.lang.Long" -> value.toString().toLongOrNull() ?: 0
                "double" -> value.toString().toDoubleOrNull() ?: 0
                "boolean" -> value.toString() == "1" || value.toString().toBoolean()
                "java.util.Date" -> if (value is Date) value else Date(LocalDateTime.parse(value.toString()).toInstant(ZoneOffset.UTC).epochSecond*1000)
                else -> null
            }
        }

        private fun getRelationFieldValue(field: Field, value:Any, entity:Any):Any? {
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

        fun getLastUpdateTimestamp(entityName:String):Long {
            return (
                (entityManager.createQuery("SELECT e FROM $entityName e ORDER BY updatedAt desc").resultList[0]
                        as? YModel)?.updatedAt?.time ?: 0
                )/1000
        }


    }
}