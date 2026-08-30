package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountType
import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class SystemAccount(
    override val code: String,
    val accountName: String,
    val type: AccountType,
    val parent: SystemAccount? = null
) : HasCode {

    // Assets
    ASSETS("001", "Assets", AccountType.ASSET),

    // Current Assets
    CURRENT_ASSETS("001.001", "Current Assets", AccountType.ASSET, parent = ASSETS),
    CASH("001.001.001", "Cash", AccountType.ASSET, parent = CURRENT_ASSETS),
    ACCOUNTS_RECEIVABLE("001.001.002", "Accounts Receivable", AccountType.ASSET, parent = CURRENT_ASSETS),
    TRADE_RECEIVABLES("001.001.002.001", "Trade Receivables", AccountType.ASSET, parent = ACCOUNTS_RECEIVABLE),
    ALLOWANCE_FOR_DOUBTFUL_ACCOUNTS("001.001.002.002", "Allowance for Doubtful Accounts", AccountType.ASSET_CONTRA, parent = ACCOUNTS_RECEIVABLE),
    INVENTORY("001.001.003", "Inventory", AccountType.ASSET, parent = CURRENT_ASSETS),
    TAX_RECOVERABLE("001.001.004", "Tax Recoverable", AccountType.ASSET, parent = CURRENT_ASSETS),
    DIGITAL_PAYMENTS("001.001.005", "Digital Payments", AccountType.ASSET, parent = CURRENT_ASSETS),

    // Fixed Assets
    FIXED_ASSETS("001.002", "Fixed Assets", AccountType.ASSET, parent = ASSETS),
    FURNITURE_AND_FIXTURES("001.002.001", "Furniture & Fixtures", AccountType.ASSET, parent = FIXED_ASSETS),
    EQUIPMENT("001.002.002", "Equipment", AccountType.ASSET, parent = FIXED_ASSETS),
    VEHICLES("001.002.003", "Vehicles", AccountType.ASSET, parent = FIXED_ASSETS),
    BUILDINGS("001.002.004", "Buildings", AccountType.ASSET, parent = FIXED_ASSETS),
    LAND("001.002.005", "Land", AccountType.ASSET, parent = FIXED_ASSETS),
    ACCUMULATED_DEPRECIATION("001.002.006", "Accumulated Depreciation", AccountType.ASSET_CONTRA, parent = FIXED_ASSETS),

    // Liabilities
    LIABILITIES("002", "Liabilities", AccountType.LIABILITY),
    ACCOUNTS_PAYABLE("002.001", "Accounts Payable", AccountType.LIABILITY, parent = LIABILITIES),
    TRADE_PAYABLES("002.001.001", "Trade Payables", AccountType.LIABILITY, parent = ACCOUNTS_PAYABLE),
    PURCHASE_DISCOUNTS("002.001.002", "Purchase Discounts", AccountType.LIABILITY_CONTRA, parent = ACCOUNTS_PAYABLE),
    TAX_PAYABLE("002.002", "Tax Payable", AccountType.LIABILITY, parent = LIABILITIES),
    WAGES_PAYABLE("002.003", "Wages Payable", AccountType.LIABILITY, parent = LIABILITIES),

    // Equity
    EQUITY("003", "Equity", AccountType.EQUITY),
    OWNERS_CAPITAL("003.001", "Owner's Capital", AccountType.EQUITY, parent = EQUITY),
    RETAINED_EARNINGS("003.002", "Retained Earnings", AccountType.EQUITY, parent = EQUITY),
    OWNERS_DRAWS("003.003", "Owner's Draws", AccountType.EQUITY_CONTRA, parent = EQUITY),
    OPENING_BALANCE_EQUITY("003.004", "Opening Balance Equity", AccountType.EQUITY, parent = EQUITY),

    // Revenue
    REVENUE("004", "Revenue", AccountType.REVENUE),
    SALES_REVENUE("004.001", "Sales Revenue", AccountType.REVENUE, parent = REVENUE),
    GROSS_SALES("004.001.001", "Gross Sales", AccountType.REVENUE, parent = SALES_REVENUE),
    SALES_RETURNS("004.001.002", "Sales Returns", AccountType.REVENUE_CONTRA, parent = SALES_REVENUE),
    SALES_ALLOWANCES("004.001.003", "Sales Allowances", AccountType.REVENUE_CONTRA, parent = SALES_REVENUE),
    SALES_DISCOUNTS("004.001.004", "Sales Discounts", AccountType.REVENUE_CONTRA, parent = SALES_REVENUE),
    OTHER_INCOME("004.002", "Other Income", AccountType.REVENUE, parent = REVENUE),

    // Expenses
    EXPENSES("005", "Expenses", AccountType.EXPENSE),
    COST_OF_GOODS_SOLD("005.001", "Cost of Goods Sold", AccountType.EXPENSE, parent = EXPENSES),
    WAGES_EXPENSE("005.002", "Wages Expense", AccountType.EXPENSE, parent = EXPENSES),
    RENT_EXPENSE("005.003", "Rent Expense", AccountType.EXPENSE, parent = EXPENSES),
    UTILITIES_EXPENSE("005.004", "Utilities Expense", AccountType.EXPENSE, parent = EXPENSES),
    INBOUND_SHIPPING("005.005", "Inbound Shipping", AccountType.EXPENSE, parent = EXPENSES),
    SUPPLIER_DELIVERY_CHARGES("005.005.001", "Supplier Delivery Charges", AccountType.EXPENSE, parent = INBOUND_SHIPPING),
    THIRD_PARTY_FREIGHT("005.005.002", "Third-Party Freight/Haulage", AccountType.EXPENSE, parent = INBOUND_SHIPPING),
    OUTBOUND_SHIPPING("005.006", "Outbound Shipping", AccountType.EXPENSE, parent = EXPENSES),
    OFFICE_SUPPLIES("005.007", "Office Supplies", AccountType.EXPENSE, parent = EXPENSES),
    STAFF_WELFARE("005.008", "Staff Welfare", AccountType.EXPENSE, parent = EXPENSES),
    EQUIPMENT_AND_ELECTRONICS("005.009", "Equipment & Electronics", AccountType.EXPENSE, parent = EXPENSES),
    REPAIRS_AND_MAINTENANCE("005.010", "Repairs & Maintenance", AccountType.EXPENSE, parent = EXPENSES),
    SHRINKAGE_AND_LOSSES("005.011", "Shrinkage & Losses", AccountType.EXPENSE, parent = EXPENSES),
    BAD_DEBT_EXPENSE("005.012", "Bad Debt Expense", AccountType.EXPENSE, parent = EXPENSES),
    OTHER_OPERATING_EXPENSES("005.013", "Other Operating Expenses", AccountType.EXPENSE, parent = EXPENSES);

    fun isSingleLevelExtensionPoint(): Boolean = this in singleLevelExtensionPoints

    companion object {
        // Accepts exactly one level of user-created children directly beneath it (e.g. a payment
        // account under DIGITAL_PAYMENTS) — a second level is blocked separately, by
        // ChildAccountCreator.preventSystemAccountGainingGrandChild. This is NOT an inherited
        // trait: a child of one of these accounts is not itself an extension point.
        private val singleLevelExtensionPoints = setOf(DIGITAL_PAYMENTS, TAX_RECOVERABLE, TAX_PAYABLE)
        private val byCode = entries.associateBy { it.code }
        fun fromCode(code: String): SystemAccount? = byCode[code]
    }
}
