package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class PriceAdjustmentMethod(override val code: String) : HasCode {
    PERCENTAGE("PCT"),

    /** The value is the whole line amount, never multiplied by a quantity. Only a sale price override uses it. */
    FIXED_LINE_TOTAL("FLT")
}
