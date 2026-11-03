package me.ezra_home.retail_software_solution.organizations.business.account.api


data class AccountRootCreateRequest(
    val name: String,
    val accountType: AccountType
)
