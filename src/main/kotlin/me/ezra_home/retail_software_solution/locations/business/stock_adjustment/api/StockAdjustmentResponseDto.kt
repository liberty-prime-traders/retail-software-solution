package me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.StripZeroesAndRound
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentCategory
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class StockAdjustmentResponseDto(
    val referenceNumber: String,
    val locationProductId: UUID,
    val productLabel: String,
    val category: StockAdjustmentCategory,
    val direction: StockAdjustmentDirection,
    @get:JsonProperty("quantity")
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    @field:JsonSerialize(using = StripZeroesAndRound::class)
    val totalCost: BigDecimal,
    val stockMovementReasonId: UUID?,
    val note: String?,
    val adjustedBy: String,
    val adjustedAt: OffsetDateTime
)
