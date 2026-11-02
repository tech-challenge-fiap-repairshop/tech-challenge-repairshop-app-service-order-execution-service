package com.cao.repairshop.core.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")

        if (header != null && header.startsWith("Bearer ")) {
            val token = header.substring(7)
            val claims = jwtService.validateToken(token)

            if (claims != null) {
                val userId = claims.subject
                val role = claims["role"] as? String ?: ""
                val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))

                val authentication = UsernamePasswordAuthenticationToken(userId, null, authorities)
                SecurityContextHolder.getContext().authentication = authentication
                log.debug("Usuário autenticado via JWT: sub={}, role={}", userId, role)
            } else {
                log.warn("Token JWT inválido ou expirado recebido para URI: {}", request.requestURI)
            }
        }

        filterChain.doFilter(request, response)
    }
}
