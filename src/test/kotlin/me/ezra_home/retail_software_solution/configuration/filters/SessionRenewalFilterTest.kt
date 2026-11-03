package me.ezra_home.retail_software_solution.configuration.filters

import me.ezra_home.retail_software_solution.configuration.security.JwtConfiguration
import me.ezra_home.retail_software_solution.configuration.security.SessionTokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtValidationException
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant
import java.util.UUID

class SessionRenewalFilterTest {

    private val secret = "test-secret-that-is-at-least-32-bytes-long"
    private val jwtConfiguration = JwtConfiguration(secret)
    private val jwtDecoder = jwtConfiguration.jwtDecoder()
    private val maxSessionMinutes = 600L
    private val sessionTokenService = SessionTokenService(jwtConfiguration.jwtEncoder(), 60L, maxSessionMinutes)
    private val sessionRenewalFilter = SessionRenewalFilter(sessionTokenService)
    private val systemUserId = UUID.randomUUID()

    @AfterEach
    fun clearSecurityContext() = SecurityContextHolder.clearContext()

    @Test
    fun `a token under half its life is not renewed`() {
        val authTime = Instant.now().minusSeconds(10 * 60)

        val response = applyFilterTo(sessionJwt(issuedMinutesAgo = 10, authTime = authTime))

        assertNull(response.getHeader(SessionRenewalFilter.RENEWED_TOKEN_HEADER))
    }

    @Test
    fun `a token past half its life is renewed and keeps the original sign-in time`() {
        val authTime = Instant.now().minusSeconds(45 * 60)

        val response = applyFilterTo(sessionJwt(issuedMinutesAgo = 45, authTime = authTime))

        val renewedJwt = jwtDecoder.decode(renewedTokenOf(response))
        assertEquals(systemUserId.toString(), renewedJwt.subject)
        assertEquals(authTime.epochSecond, renewedJwt.getClaim<Number>(SessionTokenService.AUTH_TIME_CLAIM).toLong())
        assertTrue(renewedJwt.expiresAt!!.isAfter(Instant.now()))
    }

    @Test
    fun `a renewed token never outlives the absolute session cap`() {
        val authTime = Instant.now().minusSeconds((maxSessionMinutes - 5) * 60)

        val response = applyFilterTo(sessionJwt(issuedMinutesAgo = 45, authTime = authTime))

        val renewedJwt = jwtDecoder.decode(renewedTokenOf(response))
        assertTrue(!renewedJwt.expiresAt!!.isAfter(sessionTokenService.absoluteSessionEnd(authTime)))
    }

    @Test
    fun `a token at the absolute session cap is not renewed`() {
        val authTime = Instant.now().minusSeconds(maxSessionMinutes * 60)

        val response = applyFilterTo(sessionJwt(issuedMinutesAgo = 45, authTime = authTime))

        assertNull(response.getHeader(SessionRenewalFilter.RENEWED_TOKEN_HEADER))
    }

    @Test
    fun `a token minted before auth_time existed is not renewed`() {
        val issuedAt = Instant.now().minusSeconds(45 * 60)
        val legacyJwt = Jwt.withTokenValue("legacy-token")
            .header("alg", "HS256")
            .subject(systemUserId.toString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(60 * 60))
            .build()

        val response = applyFilterTo(legacyJwt)

        assertNull(response.getHeader(SessionRenewalFilter.RENEWED_TOKEN_HEADER))
    }

    @Test
    fun `a request without an authenticated jwt gets no renewed token`() {
        val response = MockHttpServletResponse()

        sessionRenewalFilter.doFilter(MockHttpServletRequest(), response, MockFilterChain())

        assertNull(response.getHeader(SessionRenewalFilter.RENEWED_TOKEN_HEADER))
    }

    @Test
    fun `an expired token is rejected by the decoder before the renewal filter can see it`() {
        val issuedAt = Instant.now().minusSeconds(2 * 60 * 60)
        val expiredToken = jwtConfiguration.jwtEncoder().encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                    .subject(systemUserId.toString())
                    .issuedAt(issuedAt)
                    .expiresAt(issuedAt.plusSeconds(60 * 60))
                    .claim(SessionTokenService.AUTH_TIME_CLAIM, issuedAt.epochSecond)
                    .build()
            )
        ).tokenValue

        assertThrows(JwtValidationException::class.java) { jwtDecoder.decode(expiredToken) }
    }

    @Test
    fun `a request refused with 403 gets no renewed token`() {
        val authTime = Instant.now().minusSeconds(45 * 60)
        SecurityContextHolder.getContext().authentication =
            JwtAuthenticationToken(sessionJwt(issuedMinutesAgo = 45, authTime = authTime))
        val response = MockHttpServletResponse()
        val refusingChain = FilterChain { _, chainResponse -> (chainResponse as HttpServletResponse).status = 403 }

        sessionRenewalFilter.doFilter(MockHttpServletRequest(), response, refusingChain)

        assertNull(response.getHeader(SessionRenewalFilter.RENEWED_TOKEN_HEADER))
    }

    private fun sessionJwt(issuedMinutesAgo: Long, authTime: Instant): Jwt {
        val issuedAt = Instant.now().minusSeconds(issuedMinutesAgo * 60)
        return Jwt.withTokenValue("original-token")
            .header("alg", "HS256")
            .subject(systemUserId.toString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(60 * 60))
            .claim(SessionTokenService.AUTH_TIME_CLAIM, authTime.epochSecond)
            .build()
    }

    private fun applyFilterTo(jwt: Jwt): MockHttpServletResponse {
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
        val response = MockHttpServletResponse()
        sessionRenewalFilter.doFilter(MockHttpServletRequest(), response, MockFilterChain())
        return response
    }

    private fun renewedTokenOf(response: MockHttpServletResponse): String {
        val renewedToken = response.getHeader(SessionRenewalFilter.RENEWED_TOKEN_HEADER)
        assertNotNull(renewedToken)
        return renewedToken!!
    }
}
