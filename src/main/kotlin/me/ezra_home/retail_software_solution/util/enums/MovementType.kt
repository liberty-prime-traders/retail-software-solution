package me.ezra_home.retail_software_solution.util.enums

enum class MovementType(override val code: String) : HasCode {
    SOLD("SLD"),
    CUSTOMER_RETURN("CR"),
    SUPPLIER_RETURN("SR"),
    ADJUSTMENT("ADJ"),
    TRANSFER_IN("TIN"),
    TRANSFER_OUT("TOUT"),
    WRITE_OFF("WO")
}
