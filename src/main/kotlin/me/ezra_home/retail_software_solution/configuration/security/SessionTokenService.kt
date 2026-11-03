package me.ezra_home.retail_software_solution.configuration.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class SessionTokenService(
    private val jwtEncoder: JwtEncoder,

    @param:Value($$"${rts.jwt.expiration-minutes}")
    private val sessionTokenExpirationMinutes: Long,

    @param:Value($$"${rts.jwt.max-session-minutes}")
    private val sessionTokenMaxSessionMinutes: Long
) {

    fun mint(systemUserId: UUID, authTime: Instant = Instant.now()): String {
        val issuedAt = Instant.now()
        val expiresAt = minOf(
            issuedAt.plusSeconds(sessionTokenExpirationMinutes * 60),
            absoluteSessionEnd(authTime)
        )
        val claims = JwtClaimsSet.builder()
            .subject(systemUserId.toString())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim(AUTH_TIME_CLAIM, authTime.epochSecond)
            .build()
        val headers = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue
    }

    fun absoluteSessionEnd(authTime: Instant): Instant = authTime.plusSeconds(sessionTokenMaxSessionMinutes * 60)

    companion object {
        const val AUTH_TIME_CLAIM = "auth_time"
    }
}
