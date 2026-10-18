package me.ezra_home.retail_software_solution.locations.business.opening_stock.api

import me.ezra_home.retail_software_solution.configuration.serializer.StripZeroesAndRound
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class OpeningStockResponseDto(
    val referenceNumber: String,
    val locationProductId: UUID,
    val productLabel: String,
    @get:JsonProperty("quantity")
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    @field:JsonSerialize(using = StripZeroesAndRound::class)
    val unitCost: BigDecimal,
    val declaredBy: String,
    val declaredAt: OffsetDateTime
)
