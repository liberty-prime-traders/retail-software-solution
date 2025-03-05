package me.ezra_home.retail_software_solution.business.unitvalue.dto

import jakarta.validation.constraints.DecimalMax
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.UnitValueEntity}
 */
data class UnitValueResponseDto (
    val name: String?,
    val code: String?,
    val description: String?,
    val unitGroupId: UUID?,
    val baseUnit: UUID?,
    val conversionFactor: Double?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val usageCount: Long?,
) : Serializable