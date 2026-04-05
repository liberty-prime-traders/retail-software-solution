package me.ezra_home.retail_software_solution.platform.business.authorization_pass

import org.mapstruct.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class PassReferenceNumber

@Component
class AuthorizationPassQualifier(private val authorizationPassRepository: AuthorizationPassRepository) {

    @PassReferenceNumber
    fun getPassReferenceNumber(passId: UUID?): String? {
        if (passId == null) return null
        return authorizationPassRepository.findById(passId).orElse(null)?.referenceNumber
    }
}
