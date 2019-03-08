package ru.itport.yourspendings.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.persistence.EntityManager
import javax.sql.DataSource

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Autowired lateinit var restAuthenticationEntryPoint: RestAuthenticationEntryPoint

    @Autowired lateinit var usersService: UsersService

    override fun configure(http: HttpSecurity) {
        http.cors().and()
                .csrf().disable()
                .exceptionHandling()
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .and()
                .authorizeRequests()
                .antMatchers("/api/").denyAll()
                .antMatchers("/api/user/**").hasRole("ADMIN")
                .antMatchers("/api/role/**").hasRole("ADMIN")
                .antMatchers("/api/**").hasRole("USER")
                .antMatchers("/auth/profile").hasRole("USER")
                .antMatchers("/**").permitAll()
                .and()
                .formLogin()
                .and()
                .logout().logoutSuccessUrl("/")
    }

    override fun configure(auth: AuthenticationManagerBuilder?) {
        auth?.let {
            it.userDetailsService(usersService)
        }
    }

}

@Component
class CorsConfigurer: WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**").allowedOrigins("*")
                .allowedMethods("PUT", "DELETE","POST","GET", "OPTIONS")
                .allowedHeaders("Set-Cookie","Cookie","*")
                .allowCredentials(true)
    }
}

@Component
class RestAuthenticationEntryPoint : AuthenticationEntryPoint {
    override fun commence(request: HttpServletRequest?,
                          response: HttpServletResponse?,
                          authException: org.springframework.security.core.AuthenticationException?) {
        response?.status = HttpServletResponse.SC_UNAUTHORIZED
    }
}

@Service
class UsersService : JdbcDaoImpl() {

    @Autowired lateinit var dbSource: DataSource
    @Autowired lateinit var entityManager: EntityManager

    @PostConstruct
    private fun initialize() {
        setDataSource(dbSource)
    }

    override fun loadUsersByUsername(username: String): MutableList<UserDetails> =
    ArrayList<UserDetails>().apply {
        (entityManager.createQuery("SELECT u FROM User u WHERE name='$username'")
                .singleResult as? ru.itport.yourspendings.entity.User)?.let {
            add(User(it.name, it.password, it.enabled, true, true, true, AuthorityUtils.NO_AUTHORITIES))
        }
    }

    override fun loadUserAuthorities(username: String): MutableList<GrantedAuthority> =
    ArrayList<GrantedAuthority>().apply {
        (entityManager.createQuery("SELECT u FROM User u WHERE name='$username'")
                .singleResult as? ru.itport.yourspendings.entity.User)?.let { user ->
            user.roles?.forEach {role -> add(SimpleGrantedAuthority("ROLE_"+role.roleId)) }
        }
    }
}