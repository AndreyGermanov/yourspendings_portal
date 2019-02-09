package ru.itport.yourspendings.dao

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import ru.itport.yourspendings.entity.Shop

interface ShopsRepository: JpaRepository<Shop, String> {

    @Query("select s from Shop s where s.name = :name and s.id != :id ")
    fun findByNameAndNotId(@Param("name") name:String, @Param("id") id:String): Shop?

    fun findByName(name:String): Shop?
}

open class ShopValidator(val isNew:Boolean): Validator {
    override fun supports(clazz: Class<*>): Boolean = Shop::class.java.equals(clazz)

    @Autowired
    lateinit var shopsRepository: ShopsRepository

    override fun validate(target: Any, errors: Errors) {
        if (target is Shop) {
            if (target.name.isEmpty()) errors.rejectValue("name","Value is empty")
            var shop: Shop? = null
            shop = if (isNew)
                shopsRepository.findByName(target.name)
            else
                shopsRepository.findByNameAndNotId(target.name,target.id!!)
            if (shop != null) errors.rejectValue("name","Item with specified 'name' already exists")
        }
    }
}

@Component("beforeCreateShopValidator")
class ShopBeforeCreateValidator: ShopValidator(true)

@Component("beforeSaveShopValidator")
class ShopBeforeSaveValidator: ShopValidator(false)
