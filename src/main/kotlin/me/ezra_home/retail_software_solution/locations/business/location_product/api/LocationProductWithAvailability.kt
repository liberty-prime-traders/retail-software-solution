package me.ezra_home.retail_software_solution.locations.business.location_product.api

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.StripZeroesAndRound
import java.math.BigDecimal
import java.util.UUID

// quantity* figures are in the base unit, rounded so available never exceeds what can be sold; storage figures and breakdown texts are exact
data class LocationProductWithAvailability(
    val id: UUID,
    val referenceNumber: String,
    val productName: String,
    val productGroupName: String,
    val quantityOnHand: BigDecimal,
    val quantityReserved: BigDecimal,
    val quantityAvailable: BigDecimal,
    @field:JsonSerialize(using = StripZeroesAndRound::class)
    val defaultSalePrice: BigDecimal,
    val storageQuantityOnHand: BigDecimal,
    val storageQuantityReserved: BigDecimal,
    val storageQuantityAvailable: BigDecimal,
    val quantityOnHandBreakdown: String,
    val quantityAvailableBreakdown: String,
)
