package me.ezra_home.retail_software_solution.platform.business.tax_type.public

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class TaxTrigger(override val code: String) : HasCode {
    SALE("SAL"),
    SALE_RETURN("SALR"),
    CREDIT_NOTE("CRN"),
    DELIVERY("DEL"),
    PURCHASE_RETURN("PURR"),
    WRITE_OFF("WTO"),
    STOCK_ADJUSTMENT("STKAD");
}
