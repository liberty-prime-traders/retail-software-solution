package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import java.math.BigDecimal
import java.util.UUID

data class ValidatedSaleLines(
    val productSummaries: Map<UUID, LocationProductSummaryDto>,
    val factorByProductId: Map<UUID, BigDecimal>,
)
