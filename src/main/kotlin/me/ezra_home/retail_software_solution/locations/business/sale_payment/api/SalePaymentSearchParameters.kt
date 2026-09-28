package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

enum class SalePaymentStatusFilter {
  ACTIVE,
  VOIDED
}

data class SalePaymentSearchParameters(
  val recordedFrom: OffsetDateTime? = null,
  val recordedBefore: OffsetDateTime? = null,
  val paymentDateFrom: OffsetDateTime? = null,
  val paymentDateBefore: OffsetDateTime? = null,
  val contactIds: List<UUID> = emptyList(),
  val paymentMethodIds: List<UUID> = emptyList(),
  val statuses: Set<SalePaymentStatusFilter> = emptySet(),
  val minAmount: BigDecimal? = null,
  val maxAmount: BigDecimal? = null,
  val saleReferenceNumbers: List<String> = emptyList()
)
