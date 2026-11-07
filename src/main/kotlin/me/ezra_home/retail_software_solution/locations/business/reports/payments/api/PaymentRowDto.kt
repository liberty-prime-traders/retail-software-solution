package me.ezra_home.retail_software_solution.locations.business.reports.payments.api

import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PaymentRowDto(
  val id: UUID,
  val type: PaymentType,
  val referenceNumber: String,
  val contactName: String?,
  val paymentMethodName: String,
  val amount: BigDecimal,
  val balanceEffect: BalanceEffect,
  val paymentDate: OffsetDateTime,
  val recordedOn: OffsetDateTime,
  val voided: Boolean,
  val voidedReason: String?
)
