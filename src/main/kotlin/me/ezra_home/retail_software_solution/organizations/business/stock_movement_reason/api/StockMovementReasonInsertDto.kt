package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api

import me.ezra_home.retail_software_solution.locations.business.stock.api.MovementType

data class StockMovementReasonInsertDto(
    val name: String,
    val description: String? = null,
    val movementTypes: Set<MovementType>
)
