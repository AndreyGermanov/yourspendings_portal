package ru.itport.yourspendings.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.FilterInvocation
import org.springframework.security.web.access.expression.WebSecurityExpressionRoot
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.config.SpringDataRestConfig
import ru.itport.yourspendings.dao.UsersRepository

@RestController
@RequestMapping("/auth")
class AuthController {

    @Autowired lateinit var restConfig: SpringDataRestConfig
    @Autowired lateinit var links: RepositoryEntityLinks
    @Autowired lateinit var users: UsersRepository

    @GetMapping("/profile")
    fun getProfile(auth: Authentication):HashMap<String,Any> = hashMapOf(
        "roles" to auth.authorities.map { it.authority },
        "username" to (auth.principal as User).username,
        "modules" to restConfig.entities.filter {
            var result = false
            (it["interface"]!!.annotations.firstOrNull {
                it.annotationClass.simpleName == "PreAuthorize"
            } as? PreAuthorize)?.let {
                result = SpelExpressionParser().parseExpression(it.value).getValue(
                    WebSecurityExpressionRoot(auth,FilterInvocation("",""))
                ).toString().toBoolean()
            }
            result
        }.map { links.linkToCollectionResource(it["entity"]).rel }
    )
}

