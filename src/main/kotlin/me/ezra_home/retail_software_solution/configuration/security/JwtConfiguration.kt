package me.ezra_home.retail_software_solution.configuration.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtConfiguration(
    @param:Value("\${rts.jwt.secret}")
    private val jwtSecret: String
) {

    // HS256 needs a 256-bit (32 byte) key at minimum; rts.jwt.secret must be at least that long.
    private fun sessionTokenSecretKey() = SecretKeySpec(jwtSecret.toByteArray(), "HmacSHA256")

    @Bean
    fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret(sessionTokenSecretKey()))

    @Bean
    fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withSecretKey(sessionTokenSecretKey()).build()
}
