package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSearchParameters(
  val createdFrom: OffsetDateTime? = null,
  val createdBefore: OffsetDateTime? = null,
  val contactIds: List<UUID> = emptyList(),
  val soldByUserIds: List<UUID> = emptyList(),
  val saleStatuses: Set<SaleStatus> = emptySet(),
  val paymentStatuses: Set<PaymentStatus> = emptySet(),
  val minReceivableTotal: BigDecimal? = null,
  val maxReceivableTotal: BigDecimal? = null,
  val minDiscountTotal: BigDecimal? = null,
  val maxDiscountTotal: BigDecimal? = null,
  val saleReferenceNumbers: List<String> = emptyList()
)
