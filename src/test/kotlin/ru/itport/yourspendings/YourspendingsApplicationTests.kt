package ru.itport.yourspendings

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import ru.itport.yourspendings.dao.ShopsRepository

@RunWith(SpringRunner::class)
@SpringBootTest
class YourspendingsApplicationTests {

	@Autowired
	lateinit var shopsRepository: ShopsRepository
	@Test
	fun contextLoads() {
	}

	@Test
	fun shopFindById() {
		shopsRepository.findAll().forEach {
			println(it.name)
		}
	}

}

