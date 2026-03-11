package me.ezra_home.retail_software_solution.util.enums

enum class MovementType(override val code: String) : HasCode {
    SOLD("SLD"),
    CUSTOMER_RETURN("CR"),
    SUPPLIER_RETURN("SR"),
    ADJUSTMENT("ADJ"),
    TRANSFER_IN("TI"),
    TRANSFER_OUT("TO"),
    WRITE_OFF("WO"),
    PURCHASE_RECEIVED("PR")
}
