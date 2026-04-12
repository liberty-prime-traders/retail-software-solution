package me.ezra_home.retail_software_solution.organizations.business.account.api

data class AccountUsageDto(
    val usageType: AccountUsageType,
    val references: List<String>
)
