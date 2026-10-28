package me.ezra_home.retail_software_solution.organizations.business.contact.api

import me.ezra_home.retail_software_solution.util.business.MoneyDisplay
import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class ContactResponseDto(
    val id: UUID,
    val createdBy: String,
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
    val creditLimit: MoneyDisplay? = null,
    val receivableBalance: MoneyDisplay,
    val payableBalance: MoneyDisplay,
    val netBalance: MoneyDisplay,
    val balanceEffect: BalanceEffect,
    val netOpeningBalance: MoneyDisplay,
    val netOpeningBalanceEffect: BalanceEffect,
    val notes: String? = null,
    val status: ContactStatus,
    val systemDefined: Boolean
): Serializable
