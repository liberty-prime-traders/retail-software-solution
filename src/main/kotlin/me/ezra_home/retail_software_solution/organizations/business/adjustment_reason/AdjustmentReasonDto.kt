package me.ezra_home.retail_software_solution.organizations.business.adjustment_reason

import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import java.time.OffsetDateTime
import java.util.UUID

data class AdjustmentReasonDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val name: String,
    val code: String? = null,
    val direction: AdjustmentDirection,
    val systemDefined: Boolean
)
