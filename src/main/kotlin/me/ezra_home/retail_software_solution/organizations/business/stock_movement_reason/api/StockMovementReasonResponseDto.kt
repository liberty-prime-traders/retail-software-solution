package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api

import java.time.OffsetDateTime
import java.util.UUID

data class StockMovementReasonResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val code: String,
    val name: String,
    val description: String?,
    val systemDefined: Boolean,
    val createdBy: String,
    val createdOn: OffsetDateTime
)
