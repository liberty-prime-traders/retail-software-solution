package me.ezra_home.retail_software_solution.organizations.business.contact.api

import java.io.Serializable
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

data class ContactUpdateDto(
    val id: UUID,
    val contactTypes: Optional<Set<ContactType>>? = null,
    val identityType: Optional<IdentityType>? = null,
    val firstName: Optional<String>? = null,
    val lastName: Optional<String>? = null,
    val companyName: Optional<String>? = null,
    val email: Optional<String>? = null,
    val phone: Optional<String>? = null,
    val address: Optional<String>? = null,
    val creditLimit: Optional<BigDecimal>? = null,
    val notes: Optional<String>? = null,
    val status: Optional<ContactStatus>? = null
): Serializable {

    fun applyTo(existing: ContactDto): ContactDto = existing.copy(
        contactTypes = contactTypes?.orElse(existing.contactTypes) ?: existing.contactTypes,
        firstName = firstName?.orElse(existing.firstName) ?: existing.firstName,
        lastName = lastName?.orElse(existing.lastName) ?: existing.lastName,
        companyName = companyName?.orElse(existing.companyName) ?: existing.companyName,
        email = email?.orElse(existing.email) ?: existing.email,
        phone = phone?.orElse(existing.phone) ?: existing.phone,
        address = address?.orElse(existing.address) ?: existing.address,
        creditLimit = creditLimit?.orElse(existing.creditLimit) ?: existing.creditLimit,
        notes = notes?.orElse(existing.notes) ?: existing.notes,
        status = status?.orElse(existing.status) ?: existing.status
    )
}
