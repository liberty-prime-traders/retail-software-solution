package me.ezra_home.retail_software_solution.business.unitgroup.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class UnitGroupResponseDto(
    val id: UUID?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val usageCount: Long?,
    val name: String?,
    val description: String?
) : Serializable