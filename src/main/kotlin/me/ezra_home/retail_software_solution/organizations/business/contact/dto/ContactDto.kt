package me.ezra_home.retail_software_solution.organizations.business.contact.dto

import me.ezra_home.retail_software_solution.organizations.business.contact.ContactIdentity
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactStatus
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ContactDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var contactType: ContactType,
    var firstName: String? = null,
    var lastName: String? = null,
    var companyName: String? = null,
    var email: String? = null,
    var phone: String? = null,
    var address: String? = null,
    var creditLimit: BigDecimal? = null,
    var notes: String? = null,
    var status: ContactStatus = ContactStatus.ACTIVE
) {
    val identity: ContactIdentity
        get() = if (!companyName.isNullOrBlank()) {
            ContactIdentity.Organization(companyName!!)
        } else {
            ContactIdentity.Individual(firstName.orEmpty(), lastName)
        }
}
