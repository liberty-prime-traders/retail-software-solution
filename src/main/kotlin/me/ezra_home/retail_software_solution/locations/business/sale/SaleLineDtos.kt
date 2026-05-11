package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import java.util.UUID

data class PreparedLineUpdate(
    val newLines: List<SaleLineEntity>,
    val updatedLines: List<SaleLineEntity>,
    val survivingExisting: List<SaleLineEntity>,
    val productSummaries: Map<UUID, LocationProductSummaryDto>,
) {
    val survivingLines: List<SaleLineEntity>
        get() = survivingExisting + updatedLines + newLines
}
