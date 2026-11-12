package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api

import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import java.time.LocalDate
import java.util.UUID

data class ContactOpeningSettlementCreateRequest(
    val contactId: UUID,
    val subledgerDirection: ContactSubledgerDirection,
    val paymentMethodId: UUID,
    val amount: PositiveMoney,
    val settlementDate: LocalDate,
    val note: String? = null
)

data class ContactOpeningSettlementVoidRequest(
    val settlementId: UUID,
    val reason: String? = null
)
