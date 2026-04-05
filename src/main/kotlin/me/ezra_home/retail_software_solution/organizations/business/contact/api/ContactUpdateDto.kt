package me.ezra_home.retail_software_solution.organizations.business.contact.api

import java.io.Serializable
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

data class ContactUpdateDto(
    val id: UUID,
    val contactType: Optional<ContactType>? = null,
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
): Serializable
