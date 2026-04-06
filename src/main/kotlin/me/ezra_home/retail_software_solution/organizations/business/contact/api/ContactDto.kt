package me.ezra_home.retail_software_solution.organizations.business.contact.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ContactDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val contactType: ContactType,
    val firstName: String? = null,
    val lastName: String? = null,
    val companyName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val creditLimit: BigDecimal? = null,
    val notes: String? = null,
    val status: ContactStatus = ContactStatus.ACTIVE
) {
    val identity: ContactIdentity
        get() = if (!companyName.isNullOrBlank()) {
            ContactIdentity.Organization(companyName)
        } else {
            ContactIdentity.Individual(firstName.orEmpty(), lastName)
        }
}
