package me.ezra_home.retail_software_solution.util.enums

enum class ReferenceType(override val code: String) : HasCode {
    SALE("SAL"),
    TRANSFER("TRF"),
    SUPPLIER_RETURN("SR"),
    MANUAL("MAN")
}
