package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class PriceAdjustmentMethod(override val code: String) : HasCode {
    PERCENTAGE("PCT"),

    /** The value is the unit price the cashier typed; the amount is the line total minus that price x quantity. Only a price override uses it. */
    UNIT_PRICE_OVERRIDE("UPO")
}
