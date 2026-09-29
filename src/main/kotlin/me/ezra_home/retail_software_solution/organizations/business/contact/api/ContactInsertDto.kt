package me.ezra_home.retail_software_solution.organizations.business.contact.api

import java.io.Serializable
import java.math.BigDecimal

data class ContactInsertDto(
    val contactTypes: Set<ContactType>,
    val identityType: IdentityType,
    val firstName: String? = null,
    val lastName: String? = null,
    val companyName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val creditLimit: BigDecimal? = null,
    val notes: String? = null,
    val status: ContactStatus = ContactStatus.ACTIVE
): Serializable {
    val identity: ContactIdentity
        get() = if (!companyName.isNullOrBlank()) {
            ContactIdentity.Organization(companyName)
        } else {
            ContactIdentity.Individual(firstName.orEmpty(), lastName)
        }
}
