package me.ezra_home.retail_software_solution.organizations.business.unitvalue.public

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class UnitValueResponseDto(
    val id: UUID,
    val name: String,
    val code: String,
    val description: String?,
    val baseUnit: UUID?,
    val baseUnitName: String?,
    val conversionFactor: Double?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val unitGroupId: UUID,
    val referenceNumber: String?
) : Serializable
