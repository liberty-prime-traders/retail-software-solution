package me.ezra_home.retail_software_solution.organizations.business.account.api


data class AccountInsertDto(
    val code: String,
    val name: String,
    val accountType: AccountType,
    val currencyCode: String? = null,
    val accountIsSystemMaintained: Boolean = false,
    val parentAccountCode: String? = null
)
