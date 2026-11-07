package me.ezra_home.retail_software_solution.locations.business.reports.payments

import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentType
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PaymentRow(
  val id: UUID,
  val type: PaymentType,
  val referenceNumber: String,
  val contactId: UUID,
  val paymentMethodId: UUID,
  val amount: BigDecimal,
  val paymentDate: OffsetDateTime,
  val recordedOn: OffsetDateTime,
  val voidReason: String?
)
