package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SalePaymentSearchResultDto(
  val id: UUID,
  val referenceNumber: String,
  val createdOn: OffsetDateTime,
  val paymentDate: OffsetDateTime,
  val saleId: UUID,
  val saleReferenceNumber: String,
  val contactId: UUID,
  val customerName: String,
  val paymentMethodId: UUID,
  val paymentMethodName: String,
  val amount: BigDecimal,
  val reference: String?,
  val status: SalePaymentStatusFilter,
  val voidReason: String?
)
