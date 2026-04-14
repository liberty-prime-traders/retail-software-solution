package me.ezra_home.retail_software_solution.organizations.business.account.api

data class AccountChildCreateRequest(
    val parentAccountCode: String,
    val name: String
)
