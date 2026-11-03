package me.ezra_home.retail_software_solution.configuration.filters

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.ezra_home.retail_software_solution.configuration.security.SessionTokenService
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Component
class SessionRenewalFilter(private val sessionTokenService: SessionTokenService) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val renewedToken = (SecurityContextHolder.getContext().authentication?.principal as? Jwt)?.let { renewedTokenIfDue(it) }
        chain.doFilter(request, response)
        // The header is written after the chain so that a request refused later (403 from @PreAuthorize) gets no renewal.
        if (renewedToken != null && response.status < HttpStatus.BAD_REQUEST.value() && !response.isCommitted) {
            response.setHeader(RENEWED_TOKEN_HEADER, renewedToken)
        }
    }

    private fun renewedTokenIfDue(jwt: Jwt): String? {
        val issuedAt = jwt.issuedAt ?: return null
        val expiresAt = jwt.expiresAt ?: return null
        // Tokens minted before auth_time existed have no original sign-in time, so they are never renewed.
        val authTimeEpochSeconds = jwt.getClaim<Number>(SessionTokenService.AUTH_TIME_CLAIM)?.toLong() ?: return null
        val authTime = Instant.ofEpochSecond(authTimeEpochSeconds)

        val now = Instant.now()
        val halfLifeReachedAt = issuedAt.plusSeconds(Duration.between(issuedAt, expiresAt).seconds / 2)
        if (now.isBefore(halfLifeReachedAt)) return null
        if (!now.isBefore(sessionTokenService.absoluteSessionEnd(authTime))) return null

        return sessionTokenService.mint(UUID.fromString(jwt.subject), authTime)
    }

    companion object {
        const val RENEWED_TOKEN_HEADER = "X-Renewed-Token"
    }
}
