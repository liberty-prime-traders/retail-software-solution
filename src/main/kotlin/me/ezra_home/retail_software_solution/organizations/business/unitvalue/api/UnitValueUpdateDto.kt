package me.ezra_home.retail_software_solution.organizations.business.unitvalue.api

import me.ezra_home.retail_software_solution.organizations.business.unitvalue.UnitValueDto
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
) : Serializable {

    fun applyTo(existing: UnitValueDto): UnitValueDto = existing.copy(
        name = name?.orElse(existing.name) ?: existing.name,
        code = code?.orElse(existing.code) ?: existing.code,
        description = description?.orElse(existing.description) ?: existing.description,
        baseUnit = baseUnit?.orElse(existing.baseUnit) ?: existing.baseUnit,
        conversionFactor = conversionFactor?.orElse(existing.conversionFactor) ?: existing.conversionFactor
    )
}
