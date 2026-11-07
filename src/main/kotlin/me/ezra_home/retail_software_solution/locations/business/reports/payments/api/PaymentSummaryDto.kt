package me.ezra_home.retail_software_solution.locations.business.reports.payments.api

import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import java.math.BigDecimal
import java.util.UUID

data class PaymentMethodSummaryDto(
  val paymentMethodId: UUID,
  val paymentMethodName: String,
  val activeTotal: BigDecimal,
  val voidedTotal: BigDecimal
)

data class PaymentTypeSummaryDto(
  val type: PaymentType,
  val balanceEffect: BalanceEffect,
  val activeTotal: BigDecimal,
  val voidedTotal: BigDecimal,
  val activeCount: Long,
  val voidedCount: Long,
  val methods: List<PaymentMethodSummaryDto>
)

// One entry per selected type, in source order; a type that is unchecked or excluded by a reference filter is absent.
data class PaymentSummaryResponseDto(
  val summaries: List<PaymentTypeSummaryDto>
)
