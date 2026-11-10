package me.ezra_home.retail_software_solution.locations.business.purchase.api.bulk_payment

import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseBulkPaymentCreateDto(
  val supplierId: UUID,
  val purchaseIds: Set<UUID>,
  val amount: PositiveMoney,
  val paymentMethodId: UUID,
  val paymentDate: OffsetDateTime,
  val notes: String?
)
