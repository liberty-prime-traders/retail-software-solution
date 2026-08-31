package me.ezra_home.retail_software_solution.organizations.business.opening_balance.api

import java.math.BigDecimal
import java.time.OffsetDateTime

data class OpeningBalanceRevisionDto(
    val referenceNumber: String,
    val accountCode: String,
    val amount: BigDecimal,
    val changedBy: String,
    val changedAt: OffsetDateTime
)
