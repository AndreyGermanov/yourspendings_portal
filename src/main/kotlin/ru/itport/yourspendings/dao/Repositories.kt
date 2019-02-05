package ru.itport.yourspendings.dao

import org.springframework.data.jpa.repository.JpaRepository
import ru.itport.yourspendings.entity.Purchase
import ru.itport.yourspendings.entity.PurchaseImage
import ru.itport.yourspendings.entity.Shop

interface ShopsRepository: JpaRepository<Shop,String>
interface PurchasesRepository: JpaRepository<Purchase,String>
interface PurchaseImagesRepository: JpaRepository<PurchaseImage,String>