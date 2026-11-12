package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api

import me.ezra_home.retail_software_solution.util.business.MoneyDisplay
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ContactOpeningSettlementVoidResponse(
    val voidDate: LocalDate,
    val reason: String?,
    val voidedBy: String
)

data class ContactOpeningSettlementResponse(
    val id: UUID,
    val referenceNumber: String,
    val subledgerDirection: ContactSubledgerDirection,
    val paymentAccountCode: String,
    val paymentAccountLabel: String,
    val amount: MoneyDisplay,
    val settlementDate: LocalDate,
    val note: String?,
    val recordedAt: OffsetDateTime,
    val recordedBy: String,
    val void: ContactOpeningSettlementVoidResponse?
)

data class ContactOpeningSettlementSummaryResponse(
    val contactReferenceNumber: String,
    val receivableOutstanding: MoneyDisplay,
    val payableOutstanding: MoneyDisplay,
    val settlements: List<ContactOpeningSettlementResponse>
)
