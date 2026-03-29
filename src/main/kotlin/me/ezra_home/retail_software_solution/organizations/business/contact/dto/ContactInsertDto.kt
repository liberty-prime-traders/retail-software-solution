package me.ezra_home.retail_software_solution.organizations.business.contact.dto

import me.ezra_home.retail_software_solution.organizations.business.contact.ContactStatus
import me.ezra_home.retail_software_solution.organizations.business.contact.ContactType
import java.io.Serializable
import java.math.BigDecimal

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.ContactEntity}
 */
data class ContactInsertDto(
    val contactType: ContactType,
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
): Serializable
