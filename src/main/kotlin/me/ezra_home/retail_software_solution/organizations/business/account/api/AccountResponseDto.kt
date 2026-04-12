package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountType
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class AccountResponseDto(
    val id: UUID,
    val code: String,
    val name: String,
    val displayName: String,
    val accountType: AccountType,
    val accountIsActive: Boolean,
    val accountIsSystemMaintained: Boolean,
    val currentBalance: BigDecimal,
    val balanceUpdatedAt: OffsetDateTime?,
    val parentAccountId: UUID?,
    val parentAccount: String?,
    val accountIsExtensible: Boolean
) : Serializable
