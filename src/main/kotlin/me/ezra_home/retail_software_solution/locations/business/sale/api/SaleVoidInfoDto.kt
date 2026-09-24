package me.ezra_home.retail_software_solution.locations.business.sale.api

import java.time.OffsetDateTime

data class SaleVoidInfoDto(
    val voidedBy: String,
    val voidedOn: OffsetDateTime,
    val voidedReason: String?
)
