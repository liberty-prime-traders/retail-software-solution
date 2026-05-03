package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import java.math.BigDecimal
import java.util.UUID

data class SaleLineResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val locationProductId: UUID,
    val locationProduct: LocationProductSummaryDto,
    val quantity: BigDecimal,
    val unitId: UUID,
    val conversionFactor: BigDecimal,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal
)
