package me.ezra_home.retail_software_solution.organizations.business.contact.api

import me.ezra_home.retail_software_solution.util.business.Money
import java.io.Serializable

data class ContactInsertDto(
    val contactTypes: Set<ContactType>,
    val identityType: IdentityType,
    val firstName: String? = null,
    val lastName: String? = null,
    val companyName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val creditLimit: Money? = null,
    val notes: String? = null,
    val status: ContactStatus = ContactStatus.ACTIVE
): Serializable {
    val identity: ContactIdentity = if (!companyName.isNullOrBlank()) {
        ContactIdentity.Organization(companyName)
    } else {
        ContactIdentity.Individual(firstName.orEmpty(), lastName)
    }
}
