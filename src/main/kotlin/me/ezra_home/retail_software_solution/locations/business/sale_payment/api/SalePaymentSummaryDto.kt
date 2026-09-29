package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import java.math.BigDecimal
import java.util.UUID

data class SalePaymentMethodSummaryDto(
  val paymentMethodId: UUID,
  val paymentMethodName: String,
  val activeTotal: BigDecimal,
  val voidedTotal: BigDecimal
)

data class SalePaymentSummaryResponseDto(
  val methods: List<SalePaymentMethodSummaryDto>,
  val grandActiveTotal: BigDecimal,
  val grandVoidedTotal: BigDecimal,
  val activeCount: Long,
  val voidedCount: Long
)
