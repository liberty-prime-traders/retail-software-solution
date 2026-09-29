package me.ezra_home.retail_software_solution.locations.business.sale_payment.search

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.util.business.mappers.DateQualifier
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object SalePaymentSearchRowMapper {

  fun fromTuple(tuple: Tuple): SalePaymentSearchRawRow = SalePaymentSearchRawRow(
    id = tuple.get("id", UUID::class.java),
    referenceNumber = tuple.get("reference_number", String::class.java),
    createdOn = requireNotNull(DateQualifier.toOffsetDateTime(tuple.get("created_on", Instant::class.java))),
    paymentDate = requireNotNull(DateQualifier.toOffsetDateTime(tuple.get("payment_date", Instant::class.java))),
    saleId = tuple.get("sale_id", UUID::class.java),
    saleReferenceNumber = tuple.get("sale_reference_number", String::class.java),
    contactId = tuple.get("contact_id", UUID::class.java),
    paymentMethodId = tuple.get("payment_method_id", UUID::class.java),
    amount = tuple.get("amount", BigDecimal::class.java),
    reference = tuple.get("reference", String::class.java),
    voidReason = tuple.get("void_reason", String::class.java)
  )

  fun summaryFromTuple(tuple: Tuple): SalePaymentMethodSummaryRawRow = SalePaymentMethodSummaryRawRow(
    paymentMethodId = tuple.get("payment_method_id", UUID::class.java),
    activeTotal = tuple.get("active_total", BigDecimal::class.java),
    voidedTotal = tuple.get("voided_total", BigDecimal::class.java),
    activeCount = tuple.get("active_count", Number::class.java).toLong(),
    voidedCount = tuple.get("voided_count", Number::class.java).toLong()
  )
}
