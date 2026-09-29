package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class MovementReason(override val code: String, val displayName: String, val description: String) : HasCode {
    DAMAGED("DMG", "Damaged", "Stock lost or written off due to physical damage"),
    QUALITY_ISSUE("QI", "Quality Issue", "Stock removed due to quality concerns"),
    SURPLUS("SUR", "Surplus", "Excess stock removed or redistributed"),
    THEFT("THF", "Theft", "Stock lost due to theft or unexplained shrinkage"),
    COUNT_DISCREPANCY("CD", "Count Discrepancy", "Adjustment to reconcile physical stock count with system records"),
    EXPIRED("EXP", "Expired", "Stock removed because it has passed its expiry date")
}
