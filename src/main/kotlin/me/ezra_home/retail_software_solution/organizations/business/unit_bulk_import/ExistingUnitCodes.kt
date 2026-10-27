package me.ezra_home.retail_software_solution.organizations.business.unit_bulk_import

import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueResponseDto
import java.util.UUID

internal class ExistingUnitCodes(existingUnitValues: Collection<UnitValueResponseDto>) {
    val codes: Set<String> = existingUnitValues.map { it.code }.toSet()
    val systemDefinedCodes: Set<String> = existingUnitValues.filter { it.systemDefined }.map { it.code }.toSet()
    val codeById: Map<UUID, String> = existingUnitValues.associate { it.id to it.code }
}
