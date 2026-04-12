package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountType
import java.util.UUID

data class AccountInsertDto(
    val code: String,
    val name: String,
    val accountType: AccountType,
    val currencyCode: String? = null,
    val accountIsSystemMaintained: Boolean = false,
    val parentAccountId: UUID? = null
)
