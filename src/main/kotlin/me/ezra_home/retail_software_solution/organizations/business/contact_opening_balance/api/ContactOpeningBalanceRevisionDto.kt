package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api

import me.ezra_home.retail_software_solution.util.ui_models.BalanceSignal
import java.math.BigDecimal
import java.time.OffsetDateTime

data class ContactOpeningBalanceRevisionDto(
    val referenceNumber: String,
    val contactReferenceNumber: String,
    val subledgerDirection: ContactSubledgerDirection,
    val amount: BigDecimal,
    val balanceSignal: BalanceSignal,
    val changedAt: OffsetDateTime,
    val changedBy: String
)
