package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class LedgerSourceType(override val code: String) : HasCode {
    PURCHASE_DELIVERY("PD"),
    SALE("SL"),
    SALE_VOID("SLV"),
    SALE_PAYMENT("SLP"),
    SALE_PAYMENT_VOID("SLPV"),
    SUPPLIER_PAYMENT("SP"),
    SUPPLIER_PAYMENT_VOID("SPV"),
    OPENING_BALANCE("OB"),
    CONTACT_OPENING_BALANCE("COB"),
    OPENING_STOCK("OS"),
    STOCK_ADJUSTMENT("SA"),
    EXPENSE("EXP"),
    EXPENSE_VOID("EXPV"),
    EXPENSE_PAYMENT("EXPPY"),
    EXPENSE_PAYMENT_VOID("EXPPV"),
    FUND_MOVEMENT("FM"),
    FUND_MOVEMENT_VOID("FMV")
}
