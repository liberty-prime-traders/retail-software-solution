package me.ezra_home.retail_software_solution.locations.business.location_product

import java.util.UUID

interface LocationProductSummaryView {
    val id: UUID
    val referenceNumber: String
    val orgProductId: UUID
    val productName: String
    val productGroupName: String
    val baseUnitId: UUID
}
