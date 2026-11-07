package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class SaleLineCogsStatus(override val code: String) : HasCode {
    ACTIVE("ACT"),
    REPLACED("RPL")
}
