package me.ezra_home.retail_software_solution.business.unit.dto

import me.ezra_home.retail_software_solution.model.enums.DataType
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.UnitEntity}
 */
data class UnitResponseDto (
    val id: UUID?,
    val dataType: DataType?,
    val name: String?,
    val code: String?,
    val decimalCount: Short?,
    val enumerated: Boolean?,
    val enumerationOptions: List<String>?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val usageCount: Long?
) : Serializable