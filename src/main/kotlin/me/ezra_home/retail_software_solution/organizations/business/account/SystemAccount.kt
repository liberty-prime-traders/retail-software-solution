package me.ezra_home.retail_software_solution.organizations.business.account

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class SystemAccount(
    override val code: String,
    val accountName: String,
    val type: AccountType,
    val isPostable: Boolean = true,
    val parent: SystemAccount? = null
): HasCode {

    // Current Assets
    CURRENT_ASSETS("A-1000", "Current Assets", AccountType.ASSET, isPostable = false),
    CASH("A-1100", "Cash", AccountType.ASSET, parent = CURRENT_ASSETS),
    ACCOUNTS_RECEIVABLE("A-1200", "Accounts Receivable", AccountType.ASSET, isPostable = false, parent = CURRENT_ASSETS),
    TRADE_RECEIVABLES("A-1210", "Trade Receivables", AccountType.ASSET, parent = ACCOUNTS_RECEIVABLE),
    ALLOWANCE_FOR_DOUBTFUL_ACCOUNTS("A-1220", "Allowance for Doubtful Accounts", AccountType.ASSET_CONTRA, parent = ACCOUNTS_RECEIVABLE),
    INVENTORY("A-1300", "Inventory", AccountType.ASSET, parent = CURRENT_ASSETS),
    TAX_RECOVERABLE("A-1400", "Tax Recoverable", AccountType.ASSET, isPostable = false, parent = CURRENT_ASSETS),
    DIGITAL_PAYMENTS("A-1500", "Digital Payments", AccountType.ASSET, isPostable = false, parent = CURRENT_ASSETS),

    // Fixed Assets
    FIXED_ASSETS("A-2000", "Fixed Assets", AccountType.ASSET, isPostable = false),
    FURNITURE_AND_FIXTURES("A-2100", "Furniture & Fixtures", AccountType.ASSET, parent = FIXED_ASSETS),
    EQUIPMENT("A-2200", "Equipment", AccountType.ASSET, parent = FIXED_ASSETS),
    VEHICLES("A-2300", "Vehicles", AccountType.ASSET, parent = FIXED_ASSETS),
    BUILDINGS("A-2400", "Buildings", AccountType.ASSET, parent = FIXED_ASSETS),
    LAND("A-2500", "Land", AccountType.ASSET, parent = FIXED_ASSETS),
    ACCUMULATED_DEPRECIATION("A-2600", "Accumulated Depreciation", AccountType.ASSET_CONTRA, parent = FIXED_ASSETS),

    // Liabilities
    LIABILITIES("L-1000", "Liabilities", AccountType.LIABILITY, isPostable = false),
    ACCOUNTS_PAYABLE("L-1100", "Accounts Payable", AccountType.LIABILITY, isPostable = false, parent = LIABILITIES),
    TRADE_PAYABLES("L-1110", "Trade Payables", AccountType.LIABILITY, parent = ACCOUNTS_PAYABLE),
    PURCHASE_DISCOUNTS("L-1120", "Purchase Discounts", AccountType.LIABILITY_CONTRA, parent = ACCOUNTS_PAYABLE),
    TAX_PAYABLE("L-1200", "Tax Payable", AccountType.LIABILITY, isPostable = false, parent = LIABILITIES),
    WAGES_PAYABLE("L-1300", "Wages Payable", AccountType.LIABILITY, parent = LIABILITIES),

    // Equity
    EQUITY("E-1000", "Equity", AccountType.EQUITY, isPostable = false),
    OWNERS_CAPITAL("E-1100", "Owner's Capital", AccountType.EQUITY, parent = EQUITY),
    RETAINED_EARNINGS("E-1200", "Retained Earnings", AccountType.EQUITY, parent = EQUITY),
    OWNERS_DRAWS("E-1300", "Owner's Draws", AccountType.EQUITY_CONTRA, parent = EQUITY),

    // Revenue
    REVENUE("R-1000", "Revenue", AccountType.REVENUE, isPostable = false),
    SALES_REVENUE("R-1100", "Sales Revenue", AccountType.REVENUE, isPostable = false, parent = REVENUE),
    GROSS_SALES("R-1110", "Gross Sales", AccountType.REVENUE, parent = SALES_REVENUE),
    SALES_RETURNS("R-1120", "Sales Returns", AccountType.REVENUE_CONTRA, parent = SALES_REVENUE),
    SALES_ALLOWANCES("R-1130", "Sales Allowances", AccountType.REVENUE_CONTRA, parent = SALES_REVENUE),
    SALES_DISCOUNTS("R-1140", "Sales Discounts", AccountType.REVENUE_CONTRA, parent = SALES_REVENUE),
    OTHER_INCOME("R-1200", "Other Income", AccountType.REVENUE, parent = REVENUE),

    // Expenses
    EXPENSES("X-1000", "Expenses", AccountType.EXPENSE, isPostable = false),
    COST_OF_GOODS_SOLD("X-1100", "Cost of Goods Sold", AccountType.EXPENSE, parent = EXPENSES),
    WAGES_EXPENSE("X-1200", "Wages Expense", AccountType.EXPENSE, parent = EXPENSES),
    RENT_EXPENSE("X-1300", "Rent Expense", AccountType.EXPENSE, parent = EXPENSES),
    UTILITIES_EXPENSE("X-1400", "Utilities Expense", AccountType.EXPENSE, parent = EXPENSES),
    INBOUND_SHIPPING("X-1500", "Inbound Shipping", AccountType.EXPENSE, isPostable = false, parent = EXPENSES),
    SUPPLIER_DELIVERY_CHARGES("X-1510", "Supplier Delivery Charges", AccountType.EXPENSE, parent = INBOUND_SHIPPING),
    THIRD_PARTY_FREIGHT("X-1520", "Third-Party Freight/Haulage", AccountType.EXPENSE, parent = INBOUND_SHIPPING),
    OUTBOUND_SHIPPING("X-1600", "Outbound Shipping", AccountType.EXPENSE, parent = EXPENSES),
    OFFICE_SUPPLIES("X-1700", "Office Supplies", AccountType.EXPENSE, parent = EXPENSES),
    STAFF_WELFARE("X-1800", "Staff Welfare", AccountType.EXPENSE, parent = EXPENSES),
    EQUIPMENT_AND_ELECTRONICS("X-1900", "Equipment & Electronics", AccountType.EXPENSE, parent = EXPENSES),
    REPAIRS_AND_MAINTENANCE("X-2000", "Repairs & Maintenance", AccountType.EXPENSE, parent = EXPENSES),
    SHRINKAGE_AND_LOSSES("X-2100", "Shrinkage & Losses", AccountType.EXPENSE, parent = EXPENSES),
    BAD_DEBT_EXPENSE("X-2200", "Bad Debt Expense", AccountType.EXPENSE, parent = EXPENSES),
    OTHER_OPERATING_EXPENSES("X-2300", "Other Operating Expenses", AccountType.EXPENSE, parent = EXPENSES);

    fun isExtensible(): Boolean {
        return this in extensibleAccounts
    }

    companion object {
        private val extensibleAccounts = listOf(DIGITAL_PAYMENTS, TAX_RECOVERABLE, TAX_PAYABLE)
        private val byCode = entries.associateBy { it.code }
        fun fromCode(code: String): SystemAccount? = byCode[code]
    }
}
