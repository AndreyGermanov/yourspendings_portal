package ru.itport.yourspendings.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.rest.core.config.RepositoryRestConfiguration
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.validation.Validator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.InitializingBean
import java.util.*
import org.springframework.http.ResponseEntity
import org.springframework.data.rest.core.RepositoryConstraintViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import ru.itport.yourspendings.entity.*

@Component
class SpringDataRestConfig : RepositoryRestConfigurerAdapter() {
    lateinit var config: RepositoryRestConfiguration
    var entities = ArrayList<HashMap<String,Class<*>>>()

    override fun configureRepositoryRestConfiguration(config: RepositoryRestConfiguration) {
        this.config = config
        with (config.exposureConfiguration) {
            forDomainType(Shop::class.java).withItemExposure {_,methods ->
                methods.disable(HttpMethod.DELETE,HttpMethod.POST)
            }.withCollectionExposure{ _, httpMethods ->
                httpMethods.disable(HttpMethod.DELETE, HttpMethod.POST)
            }
            forDomainType(Purchase::class.java).withItemExposure { _, methods ->
                methods.disable(HttpMethod.DELETE, HttpMethod.POST)
            }.withCollectionExposure{ _, httpMethods ->
                httpMethods.disable(HttpMethod.DELETE, HttpMethod.POST)
            }
            forDomainType(PurchaseUser::class.java).withItemExposure {_, httpMethods ->
                httpMethods.disable(HttpMethod.POST,HttpMethod.PUT,HttpMethod.DELETE,HttpMethod.PATCH)
            }.withCollectionExposure { _, httpMethods ->
                httpMethods.disable(HttpMethod.POST,HttpMethod.PUT,HttpMethod.DELETE,HttpMethod.PATCH)
            }
            forDomainType(PurchaseImage::class.java).withItemExposure { _, httpMethods ->
                httpMethods.disable(HttpMethod.POST,HttpMethod.PUT,HttpMethod.DELETE,HttpMethod.PATCH)
            }.withCollectionExposure { _, httpMethods ->
                httpMethods.disable(HttpMethod.POST,HttpMethod.PUT,HttpMethod.DELETE,HttpMethod.PATCH)
            }
        }
        config.exposeIdsFor(User::class.java,Shop::class.java,Purchase::class.java,Discount::class.java,PurchaseDiscount::class.java)
        config.setRepositoryDetectionStrategy {
            entities.add(hashMapOf("entity" to it.domainType,"interface" to it.repositoryInterface))
            true
        }
        config.corsRegistry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("PUT", "DELETE", "POST", "GET","OPTIONS")
                .allowCredentials(true).maxAge(3600)
    }
}

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(RepositoryConstraintViolationException::class)
    fun handleAccessDeniedException(
            ex: Exception, request: WebRequest): ResponseEntity<Any> {
        val nevEx = ex as RepositoryConstraintViolationException
        val errors = nevEx.errors.fieldErrors.map { it.field to it.code }.toMap()

        return ResponseEntity(hashMapOf("status" to "error","errors" to errors), HttpHeaders(),
                HttpStatus.NOT_ACCEPTABLE)
    }
}

@Configuration
class ValidatorEventRegister : InitializingBean {

    @Autowired
    internal var validatingRepositoryEventListener: ValidatingRepositoryEventListener? = null

    @Autowired
    private val validators: Map<String, Validator>? = null

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        val events = Arrays.asList("beforeCreate","beforeSave")
        for ((key, value) in validators!!) {
            events.stream()
                    .filter { p -> key.startsWith(p) }
                    .findFirst()
                    .ifPresent { p ->
                        validatingRepositoryEventListener!!
                                .addValidator(p, value)
                    }
        }
    }
}


