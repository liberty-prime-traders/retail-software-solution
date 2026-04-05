package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.organizations.business.contact.ContactIdentity
import me.ezra_home.retail_software_solution.organizations.business.contact.api.IdentityType
import org.mapstruct.Qualifier
import org.springframework.stereotype.Component

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class ToIdentityType

@Component
object ContactQualifier {

    @ToIdentityType
    fun mapToIdentityType(contactDto: ContactDto): IdentityType {
        return when (contactDto.identity) {
            is ContactIdentity.Organization -> IdentityType.ORGANIZATION
            is ContactIdentity.Individual -> IdentityType.INDIVIDUAL
        }
    }
}
