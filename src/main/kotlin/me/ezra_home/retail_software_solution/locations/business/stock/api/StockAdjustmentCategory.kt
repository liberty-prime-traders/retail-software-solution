package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class StockAdjustmentCategory(override val code: String) : HasCode {
    INTERNAL_USE("IU"),
    OPENING_STOCK("OS"),
    FREE_GOODS("FG"),
    OTHER("OTH")
}
