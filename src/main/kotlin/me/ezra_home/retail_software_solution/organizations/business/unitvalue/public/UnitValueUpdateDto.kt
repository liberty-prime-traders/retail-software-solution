package me.ezra_home.retail_software_solution.organizations.business.unitvalue.public

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class UnitValueUpdateDto(
    val id: UUID? = null,
    val name: Optional<String>? = null,
    val code: Optional<String>? = null,
    val description: Optional<String>? = null,
    val baseUnit: Optional<UUID>? = null,
    val conversionFactor: Optional<Double>? = null
) : Serializable
