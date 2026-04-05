package me.ezra_home.retail_software_solution.organizations.business.contact

import me.ezra_home.retail_software_solution.organizations.business.contact.dto.IdentityType
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class ContactValidator(
    private val contactCache: ContactCache
) {

    fun validateIdentity(
        identityType: IdentityType,
        firstName: String?,
        companyName: String?
    ) {
        when (identityType) {
            IdentityType.ORGANIZATION -> {
                if (companyName.isNullOrBlank()) {
                    throw RtsGenericException("Identity type ORGANIZATION requires company name")
                }
            }
            IdentityType.INDIVIDUAL -> {
                if (firstName.isNullOrBlank()) {
                    throw RtsGenericException("Identity type INDIVIDUAL requires first name")
                }
            }
        }
    }

    fun validateUniqueness(identity: ContactIdentity, excludeId: UUID? = null) {
        if (identity.normalizedKey.isBlank()) return

        val existingContact = contactCache.getAllContacts()
            .find { contact ->
                contact.id != excludeId && contact.identity.normalizedKey == identity.normalizedKey
            }

        if (existingContact != null) {
            throw RtsGenericException("A Contact with ${identity.description()} already exists")
        }
    }
}
