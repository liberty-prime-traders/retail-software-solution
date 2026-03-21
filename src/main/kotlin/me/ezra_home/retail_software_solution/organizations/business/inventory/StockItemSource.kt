package me.ezra_home.retail_software_solution.organizations.business.inventory

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class StockItemSource(override val code: String) : HasCode {
    PURCHASE("PUR"),
    CUSTOMER_RETURN("CR"),
    GIFT("GFT"),
    OPENING_STOCK("OPS"),
    TRANSFER_IN("TI")
}
