package me.ezra_home.retail_software_solution.locations.business.location_product.api

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.StripZeroesAndRound
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class LocationProductResponseDto(
  val id: UUID,
  val productName: String,
  val orgProductId: UUID,
  val description: String?,
  val productGroupName: String,
  val categoryId: UUID,
  val baseUnit: String,
  val baseUnitId: UUID,
  @field:JsonSerialize(using = StripZeroesAndRound::class)
  val defaultSalePrice: BigDecimal?,
  val minStockLevel: Int?,
  val status: ProductStatus,
  val referenceNumber: String,
  val lastSyncedAt: OffsetDateTime?,
  @field:JsonSerialize(using = StripZeroesAndRound::class)
  val lastPurchasePrice: BigDecimal?,
  @field:JsonSerialize(using = StripZeroesAndRound::class)
  val stockBalance: BigDecimal,
  @field:JsonSerialize(using = StripZeroesAndRound::class)
  val openingStockQuantity: BigDecimal? = BigDecimal.ZERO,
  @field:JsonSerialize(using = StripZeroesAndRound::class)
  val openingStockUnitCost: BigDecimal?,
  val stockBalanceBreakdown: String? = null,
  val openingStockBreakdown: String? = null
)
