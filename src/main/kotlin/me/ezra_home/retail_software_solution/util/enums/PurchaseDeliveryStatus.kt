package me.ezra_home.retail_software_solution.util.enums

enum class PurchaseDeliveryStatus(override val code: String) : HasCode {
    PROCESSING("P"),
    RECEIVED("R"),
    FAILED("F")
}
