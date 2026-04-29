package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api

data class StockMovementReasonInsertDto(
    val name: String,
    val description: String? = null
)
