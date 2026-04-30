package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class MovementType(override val code: String) : HasCode {
    SALE("SAL"),
    SALE_VOID("SLV"),
    CUSTOMER_RETURN("CR"),
    SUPPLIER_RETURN("SR"),
    ADJUSTMENT("ADJ"),
    TRANSFER_IN("TI"),
    TRANSFER_OUT("TO"),
    WRITE_OFF("WO"),
    PURCHASE_RECEIVED("PR")
}
