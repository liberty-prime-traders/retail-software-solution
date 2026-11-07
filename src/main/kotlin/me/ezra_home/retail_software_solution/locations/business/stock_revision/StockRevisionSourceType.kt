package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class StockRevisionSourceType(override val code: String) : HasCode {
    DELIVERY_LINE("DLL")
}
