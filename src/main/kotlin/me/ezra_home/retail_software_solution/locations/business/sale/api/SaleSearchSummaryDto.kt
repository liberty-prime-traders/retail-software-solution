package me.ezra_home.retail_software_solution.locations.business.sale.api

import java.math.BigDecimal

data class SaleStatusSummaryDto(
  val status: SaleStatus,
  val saleCount: Long,
  val receivableTotal: BigDecimal,
  val discountTotal: BigDecimal,
  val paidTotal: BigDecimal,
  val outstandingTotal: BigDecimal,
  val creditTotal: BigDecimal
)

data class SaleSearchSummaryResponseDto(
  val statuses: List<SaleStatusSummaryDto>,
  val confirmedReceivableTotal: BigDecimal,
  val confirmedDiscountTotal: BigDecimal,
  val paidTotal: BigDecimal,
  val outstandingTotal: BigDecimal,
  val creditTotal: BigDecimal,
  val saleCount: Long
)
