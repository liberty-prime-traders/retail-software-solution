package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSessionSummaryDto(
    val sessionId: String,
    val createdById: UUID,
    val createdByLabel: String?,
    val createdAt: OffsetDateTime,
    val lastUpdatedAt: OffsetDateTime,
    val contactId: UUID,
    val contactLabel: String,
    val saleId: UUID?,
    val lineCount: Int,
    val payableTotal: BigDecimal,
    val activeUser: ActiveSessionUser?,
)

data class ActiveSessionUser(
    val userId: UUID,
    val userLabel: String?,
    val lastSeenAt: OffsetDateTime,
)
