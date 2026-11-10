package me.ezra_home.retail_software_solution.locations.business.reports.payments

import jakarta.persistence.Tuple
import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentType
import me.ezra_home.retail_software_solution.util.business.mappers.DateQualifier
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object PaymentSearchRowMapper {

  fun toPaymentRow(tuple: Tuple) = PaymentRow(
    id = tuple.get("id", UUID::class.java),
    type = PaymentType.valueOf(tuple.get("payment_type", String::class.java)),
    parentReferenceNumber = tuple.get("reference_number", String::class.java),
    contactId = tuple.get("contact_id", UUID::class.java),
    paymentMethodId = tuple.get("payment_method_id", UUID::class.java),
    amount = tuple.get("amount", BigDecimal::class.java),
    paymentDate = requireNotNull(DateQualifier.toOffsetDateTime(tuple.get("payment_date", Instant::class.java))),
    recordedOn = requireNotNull(DateQualifier.toOffsetDateTime(tuple.get("created_on", Instant::class.java))),
    voidReason = tuple.get("void_reason", String::class.java)
  )

  fun toPaymentMethodTotals(tuple: Tuple) = PaymentMethodTotals(
    paymentMethodId = tuple.get("payment_method_id", UUID::class.java),
    activeTotal = tuple.get("active_total", BigDecimal::class.java),
    voidedTotal = tuple.get("voided_total", BigDecimal::class.java),
    activeCount = tuple.get("active_count", Number::class.java).toLong(),
    voidedCount = tuple.get("voided_count", Number::class.java).toLong()
  )
}
