package ru.itport.yourspendings.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import javax.persistence.EntityManager

open class EntityController<T>(val entityName:String) {

    @Autowired
    lateinit var entityManager: EntityManager

    @GetMapping("/list")
    open fun list():List<T> {
        return entityManager.createQuery("SELECT u FROM $entityName u").resultList.map { it as T }
    }
}