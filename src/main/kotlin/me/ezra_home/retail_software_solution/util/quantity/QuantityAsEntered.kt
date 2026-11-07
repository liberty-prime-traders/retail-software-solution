package me.ezra_home.retail_software_solution.util.quantity

import java.util.UUID

data class QuantityAsEntered(
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val userProvidedQuantityText: String,
)
