package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.util.business.DateTimes
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSession(
    val sessionId: UUID,
    val locationId: UUID,
    val saleId: UUID?,
    val saleVersion: Long?,
    val originalStatus: SaleStatus,
    val createdById: UUID,
    val createdAt: OffsetDateTime,
    val lastUpdatedAt: OffsetDateTime,
    val lastAccessedById: UUID,
    val lastAccessedAt: OffsetDateTime,
    val header: SaleSessionHeader,
    val saleLines: List<SaleSessionLine>,
    val saleAdjustments: List<SaleSessionAdjustment>,
    val salePayments: List<SaleSessionPayment>,
    val totals: SaleSessionTotals,
) {

    fun mutable(): Boolean =
        originalStatus == SaleStatus.DRAFT

    fun totalPaid() =
        salePayments.filter { it.voidedReason == null }.sumOf { it.amount }

    fun canAddPayments(): Boolean = originalStatus ==
            SaleStatus.DRAFT || originalStatus == SaleStatus.CONFIRMED

    fun markTouched(userId: UUID): SaleSession {
        val now = DateTimes.Offset.Now.organization()
        return copy(lastUpdatedAt = now, lastAccessedById = userId, lastAccessedAt = now)
    }

    fun markVisited(userId: UUID): SaleSession =
        copy(lastAccessedById = userId, lastAccessedAt = DateTimes.Offset.Now.organization())
}
