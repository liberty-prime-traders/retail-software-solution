package me.ezra_home.retail_software_solution.configuration.session

import java.util.UUID

data class LocationSession(
    val id: UUID,
    val schemaName: String,
    val timezone: String? = null
)
