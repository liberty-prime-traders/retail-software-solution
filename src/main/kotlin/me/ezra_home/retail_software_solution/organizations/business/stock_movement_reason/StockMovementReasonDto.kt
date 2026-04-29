package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import java.time.OffsetDateTime
import java.util.UUID

data class StockMovementReasonDto(
    val id: UUID,
    val referenceNumber: String,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val code: String? = null,
    val name: String,
    val description: String?,
    val systemDefined: Boolean
)
