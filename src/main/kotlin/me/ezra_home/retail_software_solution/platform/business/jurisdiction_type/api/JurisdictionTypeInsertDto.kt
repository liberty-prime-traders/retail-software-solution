package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api

import java.io.Serializable

data class JurisdictionTypeInsertDto(
    val name: String,
    val description: String? = null
) : Serializable
