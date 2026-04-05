package me.ezra_home.retail_software_solution.organizations.business.contact.dto

import me.ezra_home.retail_software_solution.organizations.business.contact.ContactIdentity
import org.mapstruct.Qualifier
import org.springframework.stereotype.Component

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class ToIdentityType

@Component
internal object ContactQualifier {

    @ToIdentityType
    fun mapToIdentityType(contactDto: ContactDto): IdentityType {
        return when (contactDto.identity) {
            is ContactIdentity.Organization -> IdentityType.ORGANIZATION
            is ContactIdentity.Individual -> IdentityType.INDIVIDUAL
        }
    }
}
