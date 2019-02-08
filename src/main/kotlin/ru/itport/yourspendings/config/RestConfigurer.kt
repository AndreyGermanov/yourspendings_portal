package ru.itport.yourspendings.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.rest.core.config.RepositoryRestConfiguration
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import ru.itport.yourspendings.entity.Purchase
import ru.itport.yourspendings.entity.Shop
import ru.itport.yourspendings.entity.User

@Configuration
class CustomRestMvcConfig: RepositoryRestConfigurerAdapter() {
    override fun configureRepositoryRestConfiguration(config: RepositoryRestConfiguration) {
        with (config.exposureConfiguration) {
            forDomainType(Shop::class.java).withItemExposure {_,methods ->
                methods.disable(HttpMethod.DELETE,HttpMethod.POST)
            }.withCollectionExposure{ _, httpMethods ->
                httpMethods.disable(HttpMethod.DELETE,HttpMethod.POST)
            }
            forDomainType(Purchase::class.java).withItemExposure { _, methods ->
                methods.disable(HttpMethod.DELETE,HttpMethod.POST)
            }.withCollectionExposure{ _, httpMethods ->
                httpMethods.disable(HttpMethod.DELETE,HttpMethod.POST)
            }
        }
        config.exposeIdsFor(User::class.java,Shop::class.java,Purchase::class.java)
    }
}

@Component
class SpringDataRestCustomization : RepositoryRestConfigurerAdapter() {
    override fun configureRepositoryRestConfiguration(config: RepositoryRestConfiguration?) {
        config!!.corsRegistry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("PUT", "DELETE", "POST", "GET","OPTIONS")
                .allowCredentials(true).maxAge(3600)
    }
}
