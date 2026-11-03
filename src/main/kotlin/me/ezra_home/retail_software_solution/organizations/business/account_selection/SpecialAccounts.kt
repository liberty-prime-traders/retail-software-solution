package me.ezra_home.retail_software_solution.organizations.business.account_selection

import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount

// Flows post to these directly; a custom expense type pointing at one would mix its entries into them.
val RESERVED_EXPENSE_ACCOUNTS: Set<SystemAccount> = setOf(
    SystemAccount.COST_OF_GOODS_SOLD,
    SystemAccount.WAGES_EXPENSE,
    SystemAccount.INBOUND_FREIGHT,
    SystemAccount.OUTBOUND_FREIGHT,
    SystemAccount.SHRINKAGE_AND_LOSSES,
    SystemAccount.INTERNAL_USE_EXPENSE,
    SystemAccount.BAD_DEBT_EXPENSE,
    SystemAccount.TAX_EXPENSE
)

// Manual journals only touch these; contact-tracking control accounts and cash are left out on purpose.
val MANUAL_JOURNAL_ACCOUNTS: Set<SystemAccount> = setOf(
    // Assets
    SystemAccount.FURNITURE_AND_FIXTURES,
    SystemAccount.EQUIPMENT,
    SystemAccount.VEHICLES,
    SystemAccount.BUILDINGS,
    SystemAccount.LAND,
    SystemAccount.ACCUMULATED_DEPRECIATION,

    // Equity
    SystemAccount.OWNERS_CAPITAL,
    SystemAccount.OWNERS_DRAWS,
    SystemAccount.OPENING_BALANCE_EQUITY,

    // Revenue
    SystemAccount.OTHER_INCOME,

    // Expenses
    SystemAccount.RENT_EXPENSE,
    SystemAccount.UTILITIES_EXPENSE,
    SystemAccount.OFFICE_SUPPLIES,
    SystemAccount.STAFF_WELFARE,
    SystemAccount.EQUIPMENT_AND_ELECTRONICS,
    SystemAccount.REPAIRS_AND_MAINTENANCE,
    SystemAccount.OTHER_OPERATING_EXPENSES,
    SystemAccount.SHRINKAGE_AND_LOSSES
)
