package me.ezra_home.retail_software_solution.organizations.business.unitgroup.api

import java.io.Serializable

data class UnitGroupInsertDto(
    val name: String,
    val description: String? = null
) : Serializable
