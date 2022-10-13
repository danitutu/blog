package com.vdt.blog

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain


@Configuration
@EnableWebSecurity
class WebSecurityConfig {

    @Value("\${\${server.ssl.certificate-private-key:false} ? true : false}")
    private var hasCert: Boolean = false

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { requests ->
                requests
                    .antMatchers("/admin/**").authenticated()
                    .anyRequest().permitAll()
            }
            .httpBasic()
        if (hasCert) {
            http.requiresChannel { it.anyRequest().requiresSecure() }
        }
        return http.build()
    }

    @Bean
    fun userDetailsService(@Value("\${app.admin.password}") adminPassword: String): UserDetailsService {
        val authorities: List<GrantedAuthority> = listOf(SimpleGrantedAuthority("USER"))
        val user: UserDetails = User("admin", adminPassword, authorities)
        return InMemoryUserDetailsManager(user)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

}