package me.ezra_home.retail_software_solution.locations.business.location_product.dto

import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import java.io.Serializable
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class LocationProductResponseDto(
  val id: UUID?,
  val productName: String?,
  val description: String?,
  val productGroupName: String?,
  val categoryId: UUID?,
  val baseUnit: String?,
  val defaultSalePrice: BigDecimal?,
  val minStockLevel: Int?,
  val status: ProductStatus?,
  val referenceNumber: String?,
  val createdBy: String?,
  val createdOn: OffsetDateTime?,
  val lastSyncedAt: OffsetDateTime?,
  val lastPurchasePrice: BigDecimal?,
  val stockBalance: BigDecimal?
) : Serializable
