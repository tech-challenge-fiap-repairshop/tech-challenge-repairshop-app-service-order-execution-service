package com.cao.repairshop.core.security

import com.cao.repairshop.core.security.UserRole
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                // Public / Swagger / OpenAPI / Actuator
                authorize("/swagger-ui/**", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize("/swagger-custom.css", permitAll)
                authorize("/actuator/**", permitAll)

                // Endpoints da Fase 4 (desenho arquitetural)
                authorize(HttpMethod.POST, "/create_service_order", permitAll)
                authorize(HttpMethod.POST, "/execute_task/**", permitAll)
                authorize(HttpMethod.POST, "/increment_execution_task/**", permitAll)

                // Service Orders & Executions
                authorize(HttpMethod.GET, "/service-orders/**", permitAll)
                authorize(HttpMethod.POST, "/service-orders/*/approve", permitAll)
                authorize("/service-orders/**", permitAll)
                authorize("/executions/**", permitAll)

                // Default
                authorize(anyRequest, permitAll)
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(10)
}
