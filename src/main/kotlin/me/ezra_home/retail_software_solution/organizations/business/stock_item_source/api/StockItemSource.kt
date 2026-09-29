package me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class StockItemSource(override val code: String, val displayName: String, val description: String) : HasCode {
    PURCHASE("PUR", "Purchase", "Stock received through a supplier purchase order"),
    CUSTOMER_RETURN("CR", "Customer Return", "Stock returned by a customer and re-shelved"),
    GIFT("GFT", "Gift", "Stock received as a gift or donation"),
    OPENING_STOCK("OPS", "Opening Stock", "Initial stock entered when setting up inventory"),
    TRANSFER_IN("TI", "Transfer In", "Stock transferred in from another location")
}
