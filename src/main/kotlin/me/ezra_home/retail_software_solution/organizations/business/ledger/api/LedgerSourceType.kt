package me.ezra_home.retail_software_solution.organizations.business.ledger.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

// postsToProfitAndLoss has no default, so a new source type cannot be added without deciding whether profit counts it.
enum class LedgerSourceType(override val code: String, val postsToProfitAndLoss: Boolean) : HasCode {
    PURCHASE_DELIVERY("PD", false),
    PURCHASE_DELIVERY_REVISION("PDR", false),
    SALE_COGS_RESTATEMENT("SCR", true),
    STOCK_ADJUSTMENT_RESTATEMENT("SAR", true),
    SALE("SL", true),
    SALE_VOID("SLV", true),
    SALE_PAYMENT("SLP", false),
    SALE_PAYMENT_VOID("SLPV", false),
    SUPPLIER_PAYMENT("SP", false),
    SUPPLIER_PAYMENT_VOID("SPV", false),
    OPENING_BALANCE("OB", false),
    CONTACT_OPENING_BALANCE("COB", false),
    CONTACT_OPENING_SETTLEMENT("COS", false),
    CONTACT_OPENING_SETTLEMENT_VOID("COSV", false),
    OPENING_STOCK("OS", false),
    STOCK_ADJUSTMENT("SA", true),
    STOCK_TRANSFER_DISPATCH("STD", false),
    STOCK_TRANSFER_RECEIPT("STR", false),
    STOCK_TRANSFER_CANCEL("STC", false),
    EXPENSE("EXP", true),
    EXPENSE_VOID("EXPV", true),
    EXPENSE_PAYMENT("EXPPY", false),
    EXPENSE_PAYMENT_VOID("EXPPV", false),
    FUND_MOVEMENT("FM", false),
    FUND_MOVEMENT_VOID("FMV", false),
    LOAN("LN", false),
    LOAN_VOID("LNV", false),
    LOAN_REPAYMENT("LNR", false),
    LOAN_REPAYMENT_VOID("LNRV", false),
    MANUAL_JOURNAL("MJ", true),
    MANUAL_JOURNAL_VOID("MJV", true);

    companion object {
        val profitAndLossSources: Set<LedgerSourceType> = entries.filter { it.postsToProfitAndLoss }.toSet()
    }
}
