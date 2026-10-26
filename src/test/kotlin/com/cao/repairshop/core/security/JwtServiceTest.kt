package com.cao.repairshop.core.security

import com.cao.repairshop.core.security.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtServiceTest {

    private val secret = "super-secret-key-for-testing-purposes-only-32chars"
    private val jwtProperties = JwtProperties(secret = secret, expiration = 86400000L)
    private val jwtService = JwtService(jwtProperties)

    private val userId = UUID.randomUUID()
    private val role = UserRole.ATTENDANT

    @Test
    fun `generateToken should return a non-blank string`() {
        val token = jwtService.generateToken(userId, role)

        assertTrue(token.isNotBlank())
    }

    @Test
    fun `validateToken with generated token should return claims with correct subject and function`() {
        val token = jwtService.generateToken(userId, role)

        val claims = jwtService.validateToken(token)

        assertNotNull(claims)
        val validClaims = requireNotNull(claims) { "Claims should not be null for a valid token" }
        assertTrue(validClaims.subject.isNotBlank())
        assertEquals(userId.toString(), validClaims.subject)
        assertEquals(role.name, validClaims.get("role", String::class.java))
    }

    @Test
    fun `validateToken with expired token should return null`() {
        val expiredProperties = JwtProperties(secret = secret, expiration = 0L)
        val expiredJwtService = JwtService(expiredProperties)
        val token = expiredJwtService.generateToken(userId, role)

        val claims = expiredJwtService.validateToken(token)

        assertNull(claims)
    }

    @Test
    fun `validateToken with malformed token should return null`() {
        val claims = jwtService.validateToken("this.is.not.a.valid.jwt.token")

        assertNull(claims)
    }
}
