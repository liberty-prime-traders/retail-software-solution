package me.ezra_home.retail_software_solution.configuration.security

import org.springframework.context.annotation.Profile
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
@Profile("!cucumber")
class JwtOktaIdExtractor : OktaIdExtractor {

    companion object {
        private const val OKTA_ID_KEY = "uid"
    }

    override fun extract(authentication: Authentication): String {
        return (authentication.principal as Jwt).claims[OKTA_ID_KEY] as String
    }
}
