package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import java.util.UUID

interface SaleLineSummaryDto : ProductLineWithPrice {
    val id: UUID?
}
