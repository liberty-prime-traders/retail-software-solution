package me.ezra_home.retail_software_solution.business.unitvalue.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID


/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.UnitValueEntity}
 */
data class UnitValueUpdateDto (
    val id: UUID? = null,
    val name: String,
    val code: String,
    val description: Optional<String>? = null,
    val unitGroupId: Optional<UUID>? = null,
    val baseUnit: Optional<UUID>? = null,
    val conversionFactor: Optional<Double>? = null
 ) : Serializable