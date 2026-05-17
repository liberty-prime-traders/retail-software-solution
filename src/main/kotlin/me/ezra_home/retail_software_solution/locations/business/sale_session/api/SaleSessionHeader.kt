package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.time.OffsetDateTime
import java.util.UUID

data class SaleSessionHeader(
    val contactId: UUID,
    val soldById: UUID?,
    val dateSold: OffsetDateTime?,
    val notes: String?,
)
