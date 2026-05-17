package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryDto
import java.math.BigDecimal
import java.util.UUID

data class LocationProductForSaleDto(
    val id: UUID,
    val referenceNumber: String,
    val productName: String,
    val productGroupName: String,
    val baseUnitId: UUID,
    val defaultSalePrice: BigDecimal?,
    val stockBatches: List<StockEntryDto>
)
