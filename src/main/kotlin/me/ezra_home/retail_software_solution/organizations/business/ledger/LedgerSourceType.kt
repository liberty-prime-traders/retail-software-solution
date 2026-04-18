package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class LedgerSourceType(override val code: String) : HasCode {
    PURCHASE_DELIVERY("PD"),
    SALE("SL"),
    SUPPLIER_PAYMENT("SP"),
    SUPPLIER_PAYMENT_VOID("SPV")
}
