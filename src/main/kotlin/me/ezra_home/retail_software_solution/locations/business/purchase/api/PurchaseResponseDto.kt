package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryResponseDto
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseResponseDto(
  val id: UUID,
  val referenceNumber: String,
  val supplierId: UUID,
  val supplierName: String?,
  val status: PurchaseStatus,
  val notes: String?,
  val dateOrdered: OffsetDateTime?,
  val orderedBy: String?,
  val orderedById: UUID?,
  val createdBy: String?,
  val createdOn: OffsetDateTime?,
  val lines: List<PurchaseLineResponseDto>,
  val orderTotal: BigDecimal,
  val deliveries: List<PurchaseDeliveryResponseDto>
) : Serializable
