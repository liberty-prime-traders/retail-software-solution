package me.ezra_home.retail_software_solution.organizations.business.unitgroup.dto

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class UnitGroupUpdateDto (
    val id: UUID? = null,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null
) : Serializable
