package me.ezra_home.retail_software_solution.organizations.business.opening_balance.api

import me.ezra_home.retail_software_solution.util.business.Money

data class OpeningBalanceUpsertDto(
    val accountCode: String,
    val newAmount: Money
)
