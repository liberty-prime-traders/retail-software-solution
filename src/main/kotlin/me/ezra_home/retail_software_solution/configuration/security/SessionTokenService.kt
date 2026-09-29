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

    @param:Value("\${rts.jwt.expiration-minutes}")
    private val sessionTokenExpirationMinutes: Long
) {

    fun mint(systemUserId: UUID): String {
        val issuedAt = Instant.now()
        val claims = JwtClaimsSet.builder()
            .subject(systemUserId.toString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(sessionTokenExpirationMinutes * 60))
            .build()
        val headers = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue
    }
}
