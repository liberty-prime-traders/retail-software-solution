package me.ezra_home.retail_software_solution.organizations.business.account.api

data class AccountLookupDto(
    val code: String,
    val accountIsActive: Boolean,
    val hasChildren: Boolean,
    val normalBalanceEntryType: EntryType
)
