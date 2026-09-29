package me.ezra_home.retail_software_solution.organizations.business.account.api

interface AccountUsageProvider {
    val usageType: AccountUsageType
    fun getReferences(accountCode: String): List<String>
}
