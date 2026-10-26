package me.ezra_home.retail_software_solution.locations.business.stock_adjustment

import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentCreateDto
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentDirection
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

object StockAdjustmentValidator {

    fun guardCategoryRequest(stockAdjustmentCreateDto: StockAdjustmentCreateDto): StockAdjustmentDirection {
        val category = stockAdjustmentCreateDto.category
        val direction = resolveDirection(stockAdjustmentCreateDto)
        guardUnitCost(stockAdjustmentCreateDto, direction)
        if (StockAdjustmentCategoryRules.requiresReason(category)) {
            if (stockAdjustmentCreateDto.stockMovementReasonId == null) {
                throw RtsGenericException("A reason is required for the Other category")
            }
        } else if (stockAdjustmentCreateDto.stockMovementReasonId != null) {
            throw RtsGenericException("A reason is only accepted for the Other category")
        }
        if (StockAdjustmentCategoryRules.requiresNote(category) && StringUtils.getValueOrNull(stockAdjustmentCreateDto.note) == null) {
            throw RtsGenericException("A note is required for this category")
        }
        return direction
    }

    private fun resolveDirection(stockAdjustmentCreateDto: StockAdjustmentCreateDto): StockAdjustmentDirection {
        val fixedDirection = StockAdjustmentCategoryRules.fixedDirection(stockAdjustmentCreateDto.category)
        val requestedDirection = stockAdjustmentCreateDto.direction
        if (fixedDirection != null) {
            if (requestedDirection != null && requestedDirection != fixedDirection) {
                throw RtsGenericException("This category only allows a stock ${fixedDirection.name.lowercase()}")
            }
            return fixedDirection
        }
        return requestedDirection ?: throw RtsGenericException("Direction is required for the Other category")
    }

    private fun guardUnitCost(stockAdjustmentCreateDto: StockAdjustmentCreateDto, direction: StockAdjustmentDirection) {
        if (StockAdjustmentCategoryRules.requiresUserSuppliedUnitCost(direction)) {
            stockAdjustmentCreateDto.requiredUnitCost()
        } else if (stockAdjustmentCreateDto.unitCost != null) {
            throw RtsGenericException("Unit cost is taken from stock for a stock decrease")
        }
    }
}
