package me.ezra_home.retail_software_solution.organizations.business.contact.mapping

import me.ezra_home.retail_software_solution.organizations.business.contact.ContactIdentity
import me.ezra_home.retail_software_solution.organizations.business.contact.dto.IdentityType
import me.ezra_home.retail_software_solution.organizations.model.ContactEntity
import org.springframework.stereotype.Component

@Component
object ContactQualifier {

    @ToIdentityType
    fun mapToIdentityType(contactEntity: ContactEntity): IdentityType {
        return when (contactEntity.identity) {
            is ContactIdentity.Organization -> IdentityType.ORGANIZATION
            is ContactIdentity.Individual -> IdentityType.INDIVIDUAL
        }
    }
}
