package com.cao.repairshop.core.security

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
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

                // Service Orders: Customer / Attendant can view and approve
                authorize(HttpMethod.GET, "/service-orders/**", hasAnyRole(UserRole.ATTENDANT.name, UserRole.CUSTOMER.name, UserRole.MECHANIC.name, UserRole.ADMIN.name))
                authorize(HttpMethod.POST, "/service-orders/*/approve", hasAnyRole(UserRole.ATTENDANT.name, UserRole.CUSTOMER.name))
                authorize(HttpMethod.POST, "/service-orders/*/extra-executions/approve", hasAnyRole(UserRole.ATTENDANT.name, UserRole.CUSTOMER.name))

                // Attendant / Admin can create service orders
                authorize(HttpMethod.POST, "/service-orders", hasAnyRole(UserRole.ATTENDANT.name, UserRole.ADMIN.name))
                authorize(HttpMethod.POST, "/service-orders/create_service_order", hasAnyRole(UserRole.ATTENDANT.name, UserRole.ADMIN.name))
                authorize(HttpMethod.POST, "/create_service_order", hasAnyRole(UserRole.ATTENDANT.name, UserRole.ADMIN.name))

                // Mechanic / Attendant / Admin can handle diagnosis, advance status, manage executions and extra executions
                authorize(HttpMethod.POST, "/service-orders/*/finish-diagnosis", hasAnyRole(UserRole.ATTENDANT.name, UserRole.MECHANIC.name, UserRole.ADMIN.name))
                authorize(HttpMethod.POST, "/service-orders/*/extra-executions", hasAnyRole(UserRole.ATTENDANT.name, UserRole.MECHANIC.name, UserRole.ADMIN.name))
                authorize(HttpMethod.PATCH, "/service-orders/*/status", hasAnyRole(UserRole.ATTENDANT.name, UserRole.MECHANIC.name, UserRole.ADMIN.name))
                authorize("/service-orders/**", hasAnyRole(UserRole.ATTENDANT.name, UserRole.ADMIN.name))

                // Executions (Mechanic / Attendant / Admin)
                authorize("/executions/**", hasAnyRole(UserRole.ATTENDANT.name, UserRole.MECHANIC.name, UserRole.ADMIN.name))
                authorize("/execute_task/**", hasAnyRole(UserRole.ATTENDANT.name, UserRole.MECHANIC.name, UserRole.ADMIN.name))
                authorize("/increment_execution_task/**", hasAnyRole(UserRole.ATTENDANT.name, UserRole.MECHANIC.name, UserRole.ADMIN.name))

                // Default: authenticated
                authorize(anyRequest, authenticated)
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }
        return http.build()
    }
}
