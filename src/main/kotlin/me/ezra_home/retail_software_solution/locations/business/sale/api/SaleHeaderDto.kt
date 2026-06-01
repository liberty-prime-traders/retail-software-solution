package me.ezra_home.retail_software_solution.locations.business.sale.api

import java.util.UUID

data class SaleHeaderDto(
    val id: UUID,
    val referenceNumber: String,
    val version: Long,
    val status: SaleStatus,
    val contactId: UUID,
    val soldById: UUID?,
    val dateSold: java.time.OffsetDateTime?,
    val notes: String?,
)
