package me.ezra_home.retail_software_solution.locations.business.location_product

import java.math.BigDecimal
import java.util.UUID

interface LocationProductIdentityView {
    val id: UUID
    val orgProductId: UUID
    val baseUnitId: UUID
}

interface LocationProductDefaultSalePriceView {
    val id: UUID
    val defaultSalePrice: BigDecimal?
}

interface LocationProductReferenceView {
    val id: UUID
    val referenceNumber: String
}
