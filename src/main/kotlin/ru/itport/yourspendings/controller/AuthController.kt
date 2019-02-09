package ru.itport.yourspendings.controller

import org.apache.tomcat.util.security.PermissionCheck
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.support.Repositories
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itport.yourspendings.config.SpringDataRestConfig

@RestController
@RequestMapping("/auth")
class AuthController {

    lateinit var permissions: PermissionCheck
    @Autowired lateinit var restConfig: SpringDataRestConfig
    @Autowired lateinit var links: RepositoryEntityLinks
    @Autowired lateinit var reps: Repositories
    @GetMapping("/profile")

    fun getProfile(authentication: Authentication):HashMap<String,Any> {
        val modules = restConfig.entities.map { links.linkToCollectionResource(it).rel }
        val roles = authentication.authorities.map { it.authority }
        return hashMapOf("modules" to modules,"roles" to roles)
    }

}