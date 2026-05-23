package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSessionSummaryDto(
    val id: UUID,
    val createdBy: String?,
    val createdAt: OffsetDateTime,
    val lastUpdatedAt: OffsetDateTime,
    val lastAccessedBy: String,
    val lastAccessedAt: OffsetDateTime,
    val contactLabel: String,
    val payableTotal: BigDecimal
)
