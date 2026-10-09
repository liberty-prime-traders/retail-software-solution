package me.ezra_home.retail_software_solution.cross_tier.expense.search

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.util.enums.PaymentStatusConverter
import me.ezra_home.retail_software_solution.util.business.mappers.DateQualifier
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object ExpenseSearchRowMapper {

    fun fromTuple(tuple: Tuple): ExpenseSearchRawRow = ExpenseSearchRawRow(
        id = tuple.get("id", UUID::class.java),
        referenceNumber = tuple.get("reference_number", String::class.java),
        createdOn = requireNotNull(DateQualifier.toOffsetDateTime(tuple.get("created_on", Instant::class.java)))
    )

    fun summaryFromTuple(tuple: Tuple): ExpenseSummaryRawRow = ExpenseSummaryRawRow(
        expenseTypeId = tuple.get("expense_type_id", UUID::class.java),
        voided = tuple.get("voided", Boolean::class.javaObjectType),
        paymentStatus = PaymentStatusConverter().convertToEntityAttribute(tuple.get("payment_status", String::class.java))!!,
        expenseCount = tuple.get("expense_count", Number::class.java).toLong(),
        amountTotal = tuple.get("amount_total", BigDecimal::class.java),
        paidTotal = tuple.get("paid_total", BigDecimal::class.java),
        outstandingTotal = tuple.get("outstanding_total", BigDecimal::class.java)
    )
}
