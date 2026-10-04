package me.ezra_home.retail_software_solution.organizations.business.account.api

import java.math.BigDecimal

data class AccountPosting(
    val accountCode: String,
    val amount: BigDecimal,
    val entryType: EntryType
)
