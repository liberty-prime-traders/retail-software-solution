package me.ezra_home.retail_software_solution.locations.business.sale_payment.search

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SalePaymentSearchRawRow(
  val id: UUID,
  val referenceNumber: String,
  val createdOn: OffsetDateTime,
  val paymentDate: OffsetDateTime,
  val saleId: UUID,
  val saleReferenceNumber: String,
  val contactId: UUID,
  val paymentMethodId: UUID,
  val amount: BigDecimal,
  val reference: String?,
  val voidReason: String?
)

data class SalePaymentMethodSummaryRawRow(
  val paymentMethodId: UUID,
  val activeTotal: BigDecimal,
  val voidedTotal: BigDecimal,
  val activeCount: Long,
  val voidedCount: Long
)
