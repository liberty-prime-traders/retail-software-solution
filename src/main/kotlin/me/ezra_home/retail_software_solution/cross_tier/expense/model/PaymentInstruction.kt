package me.ezra_home.retail_software_solution.cross_tier.expense.model

import java.time.LocalDate
import java.util.UUID

data class PaymentInstruction(
    val paymentMethodId: UUID,
    val paymentReference: String? = null,
    val paymentDate: LocalDate? = null
)
