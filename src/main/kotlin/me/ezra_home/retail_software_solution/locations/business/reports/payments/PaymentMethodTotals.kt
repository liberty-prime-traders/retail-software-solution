package me.ezra_home.retail_software_solution.locations.business.reports.payments

import java.math.BigDecimal
import java.util.UUID

data class PaymentMethodTotals(
  val paymentMethodId: UUID,
  val activeTotal: BigDecimal,
  val voidedTotal: BigDecimal,
  val activeCount: Long,
  val voidedCount: Long
)
