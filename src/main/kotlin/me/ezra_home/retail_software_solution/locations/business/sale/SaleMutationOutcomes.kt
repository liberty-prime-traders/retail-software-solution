package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentSummaryDto
import java.util.UUID

data class SaleCreateOutcome(
    val lines: List<SaleLineEntity>,
    val insertContext: SaleLinesInsertContext,
    val adjustments: List<SaleAdjustmentSummaryDto>,
)

data class SaleUpdateOutcome(
    val survivingLines: List<SaleLineEntity>,
    val productSummaries: Map<UUID, LocationProductSummaryDto>,
    val adjustments: List<SaleAdjustmentSummaryDto>,
)
