package me.ezra_home.retail_software_solution.cross_tier.expense.search

import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ExpenseSearchParameters(
    val createdFrom: OffsetDateTime? = null,
    val createdBefore: OffsetDateTime? = null,
    val expenseDateFrom: LocalDate? = null,
    val expenseDateBefore: LocalDate? = null,
    val payeeContactIds: List<UUID> = emptyList(),
    val expenseTypeIds: List<UUID> = emptyList(),
    val sourceReferences: List<String> = emptyList(),
    val paymentMethodIds: List<UUID> = emptyList(),
    val expenseReferenceNumbers: List<String> = emptyList(),
    val paymentStatuses: Set<PaymentStatus> = emptySet(),
    val voided: Boolean? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null
) {

    fun sanitized(): ExpenseSearchParameters = copy(
        sourceReferences = StringUtils.dropBlank(sourceReferences),
        expenseReferenceNumbers = StringUtils.dropBlank(expenseReferenceNumbers)
    )
}
