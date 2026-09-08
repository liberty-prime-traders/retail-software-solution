package me.ezra_home.retail_software_solution.organizations.business.opening_balance.api

import java.math.BigDecimal

data class OpeningBalanceUpsertDto(
    val accountCode: String,
    val newAmount: BigDecimal
)
