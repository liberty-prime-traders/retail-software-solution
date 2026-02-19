package me.ezra_home.retail_software_solution.util.enums

enum class PurchaseStatus(override val code: String) : HasCode {
    DRAFT("DFT"),
    ORDERED("ORD"),
    PARTIALLY_DELIVERED("PRT"),
    DELIVERED("DL"),
    CANCELED("X")
}
