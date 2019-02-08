package ru.itport.yourspendings.clouddb

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import ru.itport.yourspendings.entity.YModel

private fun <T> anyObject(): T {
    return Mockito.any()
}


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FirebaseCloudServiceTests {

    @Autowired lateinit var service: TestCloudService

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @Autowired lateinit var webClient: WebTestClient

    @Before
    fun init() {
        jdbcTemplate.execute("delete from purchase_images")
        jdbcTemplate.execute("delete from purchases")
        jdbcTemplate.execute("delete from shops")
    }

}

@Component
class TestCloudService: FirebaseCloudService() {

    lateinit var inputData: List<MutableMap<String,Any>>

    override fun <T : YModel, U> getLastData(collection: String, repository: JpaRepository<T, U>): List<MutableMap<String, Any>> {
        return inputData
    }

    fun setDemoData(data:List<MutableMap<String,Any>>) {inputData = data}
}

