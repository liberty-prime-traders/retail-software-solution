package me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class SystemAdjustmentReason(
    override val code: String,
    val displayName: String,
    val direction: AdjustmentDirection
) : HasCode {
    LOYALTY("LOY", "Loyalty Reward", AdjustmentDirection.DISCOUNT),
    BULK("BULK", "Bulk Purchase", AdjustmentDirection.DISCOUNT),
    SEASONAL("PROMO", "Seasonal Promotion", AdjustmentDirection.DISCOUNT),
    CLEARANCE("CLR", "Clearance / Excess Inventory", AdjustmentDirection.DISCOUNT),
    STAFF("STF", "Staff Discount", AdjustmentDirection.DISCOUNT),
    DAMAGED("DMG", "Damaged Goods", AdjustmentDirection.DISCOUNT),
    EARLY_PAYMENT("ELP", "Early Payment Incentive", AdjustmentDirection.DISCOUNT),
    SMALL_ORDER("SMD", "Small Order Fee", AdjustmentDirection.SURCHARGE),
    RUSH("RSH", "Rush / Priority Processing", AdjustmentDirection.SURCHARGE),
    CUSTOM("CSTM", "Custom / Other", AdjustmentDirection.BOTH);
}
