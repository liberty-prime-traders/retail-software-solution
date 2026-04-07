package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class PurchaseStatus(override val code: String) : HasCode {
    DRAFT("DFT"),
    ORDERED("ORD"),
    PARTIALLY_DELIVERED("PDL"),
    FULLY_DELIVERED("FDL"),
    CANCELED("X");
}
