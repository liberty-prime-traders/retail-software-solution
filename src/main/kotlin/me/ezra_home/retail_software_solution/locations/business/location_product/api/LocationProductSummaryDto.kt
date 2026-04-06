package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.util.model.HasReferenceNumber
import java.util.UUID

data class LocationProductSummaryDto(
    val id: UUID,
    override val referenceNumber: String?,
    val productName: String?,
    val productGroupName: String?,
    val baseUnitId: UUID?
) : HasReferenceNumber
