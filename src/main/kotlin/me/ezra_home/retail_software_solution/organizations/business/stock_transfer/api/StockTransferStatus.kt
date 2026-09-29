package me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class StockTransferStatus(override val code: String) : HasCode {
    DRAFT("DRAFT"),
    DISPATCHED("DSPCH"),
    COMPLETED("COMPD"),
    CANCELLED("CNCLD")
}
