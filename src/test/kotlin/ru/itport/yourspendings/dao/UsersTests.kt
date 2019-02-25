package ru.itport.yourspendings.dao

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import javax.persistence.EntityManager

@RunWith(SpringRunner::class)
@SpringBootTest
class UsersTests {

    @Autowired
    lateinit var users: UsersRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun rolesByNameTest() {
        val user = users.findByName("andrey")

        println(user!!.roles!!.forEach{
            println(it.roleId)
        })
    }

    @Test
    fun customQuery() {
        entityManager.createQuery("SELECT u FROM User u").resultList.forEach {
            println(it)
        }
    }
}