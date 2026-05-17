package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.time.OffsetDateTime
import java.util.UUID

data class SaleSession(
    val sessionId: String,
    val locationId: UUID,
    val saleId: UUID?,
    val saleVersion: Long?,
    val createdById: UUID,
    val createdAt: OffsetDateTime,
    val lastUpdatedAt: OffsetDateTime,
    val lastAccessedById: UUID,
    val lastAccessedAt: OffsetDateTime,
    val header: SaleSessionHeader,
    val lines: List<SaleSessionLine>,
    val adjustments: List<SaleSessionAdjustment>,
    val payments: List<SaleSessionPayment>,
    val totals: SaleSessionTotals,
) {

    fun touched(userId: UUID, at: OffsetDateTime): SaleSession =
        copy(lastUpdatedAt = at, lastAccessedById = userId, lastAccessedAt = at)

    fun visited(userId: UUID, at: OffsetDateTime): SaleSession =
        copy(lastAccessedById = userId, lastAccessedAt = at)
}
