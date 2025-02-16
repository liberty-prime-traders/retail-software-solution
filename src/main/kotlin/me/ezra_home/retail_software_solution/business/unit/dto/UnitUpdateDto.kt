package me.ezra_home.retail_software_solution.business.unit.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.UnitEntity}
 */
data class UnitUpdateDto (
    val id: UUID? = null,
    val dataType: Optional<String>? = null,
    val name: Optional<String>? = null,
    val code: Optional<String>? = null,
    val decimalCount: Optional<Short>? = null,
    val enumerated: Optional<Boolean>? = null,
    val enumerationOptions: Optional<String>? = null,
) : Serializable