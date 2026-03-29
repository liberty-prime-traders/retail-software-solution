package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class PurchaseDeliveryStatus(override val code: String) : HasCode {
    PROCESSING("P"),
    RECEIVED("R"),
    FAILED("F")
}
