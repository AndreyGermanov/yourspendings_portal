package ru.itport.yourspendings.dao

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import ru.itport.yourspendings.entity.*
import java.util.*
import javax.persistence.EntityManager
import javax.transaction.Transactional

@RunWith(SpringRunner::class)
@SpringBootTest
class DiscountsTests {

    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @Autowired lateinit var discounts: DiscountsRepository
    @Autowired lateinit var purchasesDiscounts: PurchasesDiscountsRepository
    @Autowired lateinit var purchases: PurchasesRepository
    @Autowired lateinit var purchaseUsers: PurchaseUsersRepository
    @Autowired lateinit var shops: ShopsRepository
    @Autowired lateinit var entityManager: EntityManager

    lateinit var user:PurchaseUser
    lateinit var shop:Shop

    @Before
    fun init() {
        jdbcTemplate.execute("delete from purchase_images")
        jdbcTemplate.execute("delete from purchases")
        jdbcTemplate.execute("delete from discounts")
        jdbcTemplate.execute("delete from shops")
        jdbcTemplate.execute("delete from purchase_users")

        user = PurchaseUser(id="user1",name="User 1",email="user1@email.ru",phone="112323",isDisabled=false,
                updatedAt= Date()).also { purchaseUsers.save(it)}
        shop = Shop(id="sh1",name="Shop 1",updatedAt = Date(),latitude=5.0,longitude=6.0,user=user).also {shops.save(it)}
    }

    @Test
    fun discountTest() {
        createDiscount("10%")
        assertTrue(discounts.findById(1).isPresent)
    }

    @Test
    @Transactional
    fun discountPurchaseRelationTest() {
        val purchase1 = createPurchase("shop1")
        val discount1 = createDiscount("10%")
        val discount2 = createDiscount("5%")

        val purchaseDiscount1 = PurchaseDiscount(purchase=purchase1,discount=discount1,amount=100.00).also {
            purchasesDiscounts.save(it)
        }
        val purchaseDiscount2 = PurchaseDiscount(purchase=purchase1,discount=discount2,amount=105.00).also {
            purchasesDiscounts.save(it)
        }
        entityManager.refresh(purchase1)
    }

    fun createDiscount(name:String):Discount = discounts.save(Discount(name=name))

    fun createPurchase(id:String): Purchase = purchases.save(Purchase(id=id,place=shop,date=Date(),updatedAt=Date(),user=user))

}