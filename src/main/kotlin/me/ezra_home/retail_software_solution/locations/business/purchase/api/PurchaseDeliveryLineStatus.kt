package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class PurchaseDeliveryLineStatus(override val code: String) : HasCode {
    ACTIVE("ACT"),
    REVISED("RVS")
}
