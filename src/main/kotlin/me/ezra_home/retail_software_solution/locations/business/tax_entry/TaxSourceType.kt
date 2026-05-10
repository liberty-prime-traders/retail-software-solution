package me.ezra_home.retail_software_solution.locations.business.tax_entry

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class TaxSourceType(override val code: String) : HasCode {
    SALE("SL"),
    SALE_VOID("SLV"),
    PURCHASE_DELIVERY("PDL")
}
