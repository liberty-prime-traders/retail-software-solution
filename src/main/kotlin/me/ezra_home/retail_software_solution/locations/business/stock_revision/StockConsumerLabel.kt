package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType

object StockConsumerLabel {

    fun of(movementType: MovementType): String = when (movementType) {
        MovementType.SALE -> "Sale"
        MovementType.TRANSFER_OUT -> "Transfer"
        MovementType.STOCK_ADJUSTMENT_DECREASE -> "Stock adjustment"
        MovementType.SUPPLIER_RETURN -> "Return to supplier"
        else -> movementType.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}
