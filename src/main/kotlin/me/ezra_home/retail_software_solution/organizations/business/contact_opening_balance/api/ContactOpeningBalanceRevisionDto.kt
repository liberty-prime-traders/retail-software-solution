package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api

import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import java.math.BigDecimal
import java.time.OffsetDateTime

data class ContactOpeningBalanceRevisionDto(
    val referenceNumber: String,
    val contactReferenceNumber: String,
    val subledgerDirection: ContactSubledgerDirection,
    val amount: BigDecimal,
    val balanceEffect: BalanceEffect,
    val changedAt: OffsetDateTime,
    val changedBy: String
)
