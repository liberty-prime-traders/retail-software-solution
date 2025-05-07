package me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID


/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.entity.UnitValueEntity}
 */
data class UnitValueUpdateDto (
    val id: UUID? = null,
    val name: Optional<String>? = null,
    val code: Optional<String>? = null,
    val description: Optional<String>? = null,
    val unitGroupId: Optional<UUID>? = null,
    val baseUnit: Optional<UUID>? = null,
    val conversionFactor: Optional<Double>? = null
 ) : Serializable
