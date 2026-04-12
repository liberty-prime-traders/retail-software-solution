package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountType

data class AccountRootCreateRequest(
    val name: String,
    val accountType: AccountType
)
