package me.ezra_home.retail_software_solution.locations.business.sale.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class SaleStatus(override val code: String) : HasCode {
    DRAFT("DFT"),
    CONFIRMED("CFM"),
    VOIDED("VD"),
    DISCARDED("DSC"),
}
