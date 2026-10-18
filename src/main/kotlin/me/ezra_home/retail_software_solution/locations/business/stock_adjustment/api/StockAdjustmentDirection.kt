package me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class StockAdjustmentDirection(override val code: String, val movementType: MovementType) : HasCode {
    INCREASE("INC", MovementType.STOCK_ADJUSTMENT_INCREASE),
    DECREASE("DEC", MovementType.STOCK_ADJUSTMENT_DECREASE)
}
