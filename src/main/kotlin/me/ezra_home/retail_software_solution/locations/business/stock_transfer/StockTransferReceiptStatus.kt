package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class StockTransferReceiptStatus(override val code: String) : HasCode {
    PENDING("P"),
    COMPLETED("C")
}
