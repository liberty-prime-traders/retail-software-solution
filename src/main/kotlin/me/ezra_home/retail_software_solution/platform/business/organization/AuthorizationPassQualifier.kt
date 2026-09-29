package me.ezra_home.retail_software_solution.platform.business.organization

import me.ezra_home.retail_software_solution.platform.business.authorization_pass.api.AuthorizationPassService
import org.mapstruct.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class PassReferenceNumber

@Component
class AuthorizationPassQualifier(private val authorizationPassService: AuthorizationPassService) {

    @PassReferenceNumber
    fun getPassReferenceNumber(passId: UUID?): String? =
        passId?.let { authorizationPassService.getPassReferenceNumber(it) }
}
