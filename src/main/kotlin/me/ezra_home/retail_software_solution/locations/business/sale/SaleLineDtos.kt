package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import java.util.UUID

data class SaleLineUpdateResult(
    val survivingSaleLines: List<SaleLineEntity>,
    val productSummaries: Map<UUID, LocationProductSummaryDto>,
)
