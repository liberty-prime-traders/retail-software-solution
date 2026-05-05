package me.ezra_home.retail_software_solution.organizations.business.contact.api

import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ContactResponseDto(
    val id: UUID,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val referenceNumber: String,
    val contactTypes: Set<ContactType>,
    val identityType: IdentityType,
    val firstName: String? = null,
    val lastName: String? = null,
    val companyName: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val creditLimit: BigDecimal? = null,
    val notes: String? = null,
    val status: ContactStatus,
    val systemDefined: Boolean
): Serializable
