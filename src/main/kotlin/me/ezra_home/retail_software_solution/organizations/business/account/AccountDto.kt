package me.ezra_home.retail_software_solution.organizations.business.account

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class AccountDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val code: String,
    val name: String,
    val accountType: AccountType,
    val currencyCode: String,
    val accountIsPostable: Boolean,
    val accountIsActive: Boolean,
    val accountIsSystemMaintained: Boolean,
    val currentBalance: BigDecimal,
    val balanceUpdatedAt: OffsetDateTime?,
    val parentAccountId: UUID?
) {
    val label: String get() = "$name ($code)"
}
