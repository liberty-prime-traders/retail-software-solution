package me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto

import java.io.Serializable
import java.util.UUID


/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.UnitValueEntity}
 */
data class UnitValueInsertDto(
    val name: String? = null,
    val code: String? = null,
    val description: String? = null,
    val unitGroupId: UUID? = null,
    val baseUnit: UUID? = null,
    val conversionFactor: Double? = null
) : Serializable
