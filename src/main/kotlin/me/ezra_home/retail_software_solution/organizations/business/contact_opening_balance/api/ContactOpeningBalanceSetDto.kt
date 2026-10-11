package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api

import java.math.BigDecimal

data class ContactOpeningBalanceSetDto(
    val subledgerDirection: ContactSubledgerDirection,
    val amount: BigDecimal
)
