package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class StockMovementReason(
    override val code: String,
    val displayName: String,
    val description: String,
    val allowedMovementTypes: Set<MovementType>
) : HasCode {
    DAMAGED(
        "DMG", "Damaged", "Stock lost or written off due to physical damage",
        setOf(MovementType.STOCK_ADJUSTMENT_DECREASE, MovementType.CUSTOMER_RETURN, MovementType.SUPPLIER_RETURN)
    ),
    QUALITY_ISSUE(
        "QI", "Quality Issue", "Stock removed due to quality concerns",
        setOf(MovementType.STOCK_ADJUSTMENT_DECREASE, MovementType.CUSTOMER_RETURN, MovementType.SUPPLIER_RETURN)
    ),
    THEFT(
        "THF", "Theft", "Stock lost due to theft or unexplained shrinkage",
        setOf(MovementType.STOCK_ADJUSTMENT_DECREASE)
    ),
    COUNT_DISCREPANCY(
        "CD", "Count Discrepancy", "Adjustment to reconcile physical stock count with system records",
        setOf(MovementType.STOCK_ADJUSTMENT_INCREASE, MovementType.STOCK_ADJUSTMENT_DECREASE)
    ),
    EXPIRED(
        "EXP", "Expired", "Stock removed because it has passed its expiry date",
        setOf(MovementType.STOCK_ADJUSTMENT_DECREASE, MovementType.CUSTOMER_RETURN, MovementType.SUPPLIER_RETURN)
    )
}
