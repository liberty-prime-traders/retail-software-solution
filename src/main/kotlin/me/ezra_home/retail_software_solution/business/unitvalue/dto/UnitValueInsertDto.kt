package me.ezra_home.retail_software_solution.business.unitvalue.dto

import jakarta.validation.constraints.DecimalMax
import java.io.Serializable
import java.util.UUID


/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.UnitValueEntity}
 */
data class UnitValueInsertDto(
    val name: String,
    val code: String? = null,
    val description: String? = null,
    val unitGroupId: UUID,
    val baseUnit: UUID,
    val conversionFactor: DecimalMax
) : Serializable