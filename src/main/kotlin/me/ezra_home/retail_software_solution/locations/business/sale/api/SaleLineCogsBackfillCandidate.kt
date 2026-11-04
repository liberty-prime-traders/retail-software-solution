package me.ezra_home.retail_software_solution.locations.business.sale.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleLineCogsBackfillCandidate(
    val saleLineId: UUID,
    val cogsTotal: BigDecimal,
    val dateSold: OffsetDateTime?,
)
