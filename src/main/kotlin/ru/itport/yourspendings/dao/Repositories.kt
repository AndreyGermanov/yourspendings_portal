package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.data.rest.core.config.Projection
import ru.itport.yourspendings.entity.Purchase
import ru.itport.yourspendings.entity.PurchaseImage
import ru.itport.yourspendings.entity.Shop
import ru.itport.yourspendings.entity.User

interface ShopsRepository: JpaRepository<Shop, String>

interface PurchasesRepository: JpaRepository<Purchase,String>

interface PurchaseImagesRepository: JpaRepository<PurchaseImage,String>

@RepositoryRestResource(excerptProjection = NoPassword::class)
interface UsersRepository: JpaRepository<User,String>

@Projection(name="noPassword",types=[User::class])
interface NoPassword {
    val username:String
    val enabled:Boolean
}