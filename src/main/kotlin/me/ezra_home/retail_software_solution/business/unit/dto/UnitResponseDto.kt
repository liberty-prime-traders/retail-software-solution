package me.ezra_home.retail_software_solution.business.unit.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.UnitEntity}
 */
data class UnitResponseDto (
    val id: UUID?,
    val dataType: String?,
    val name: String?,
    val code: String?,
    val decimalCount: Short?,
    val enumerated: Boolean?,
    val enumerationOptions: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val usageCount: Long?
) : Serializable