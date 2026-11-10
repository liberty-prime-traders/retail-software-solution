package me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class PurchaseDeliveryStatus(override val code: String) : HasCode {
    PROCESSING("P"),
    RECEIVED("R"),
    FAILED("F");

    fun isAppliedToStock(): Boolean = this == RECEIVED
}
