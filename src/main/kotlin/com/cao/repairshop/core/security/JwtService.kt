package com.cao.repairshop.core.security

import com.cao.repairshop.core.security.UserRole
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID

@Service
class JwtService(private val jwtProperties: JwtProperties) {

    private val logger = LoggerFactory.getLogger(JwtService::class.java)
    private val key = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())

    fun generateToken(userId: UUID, role: UserRole): String {
        val now = Date()
        val expiration = Date(now.time + jwtProperties.expiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role.name)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Claims? {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: SignatureException) {
            logger.warn("JWT signature validation failed: {}", e.message)
            null
        } catch (e: ExpiredJwtException) {
            logger.debug("JWT token expired: {}", e.message)
            null
        } catch (e: MalformedJwtException) {
            logger.warn("Malformed JWT token: {}", e.message)
            null
        } catch (e: UnsupportedJwtException) {
            logger.warn("Unsupported JWT: {}", e.message)
            null
        } catch (e: IllegalArgumentException) {
            logger.warn("JWT claims string is empty: {}", e.message)
            null
        }
    }
}
