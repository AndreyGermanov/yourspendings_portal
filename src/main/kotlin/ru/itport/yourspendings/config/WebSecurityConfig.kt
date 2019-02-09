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
import javax.sql.DataSource

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Autowired lateinit var restAuthenticationEntryPoint: RestAuthenticationEntryPoint

    @Autowired lateinit var dataSource: DataSource

    override fun configure(http: HttpSecurity) {
        http.cors().and()
                .csrf().disable()
                .exceptionHandling()
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .and()
                .authorizeRequests()
                .antMatchers("/api/").denyAll()
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
            it.jdbcAuthentication().dataSource(dataSource)
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

