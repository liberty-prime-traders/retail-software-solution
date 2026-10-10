package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerSourceType
import me.ezra_home.retail_software_solution.util.ui_models.BalanceSignal
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SubledgerEntryResponse(
    val id: UUID,
    val createdOn: OffsetDateTime,
    val groupReferenceNumber: String,
    val sourceType: LedgerSourceType,
    val sourceReferenceNumber: String,
    val transactionAmount: BigDecimal,
    val transactionBalanceSignal: BalanceSignal,
    val netBalance: BigDecimal,
    val netBalanceSignal: BalanceSignal
)
