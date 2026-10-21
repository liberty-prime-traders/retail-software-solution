package me.ezra_home.retail_software_solution.organizations.business.opening_balance

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountLookupDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

object OpeningBalanceAccountValidator {

    private val manualOpeningBalanceRejections = mapOf(
        SystemAccount.TRADE_RECEIVABLES.code to "is driven by contact opening balances",
        SystemAccount.TRADE_PAYABLES.code to "is driven by contact opening balances",
        SystemAccount.LOANS_PAYABLE.code to "is driven only by loans"
    )

    fun assertManualOpeningBalanceAccepted(accountCode: String) {
        val rejectionReason = manualOpeningBalanceRejections[accountCode] ?: return
        throw RtsGenericException("Account $accountCode $rejectionReason and cannot take a manual opening balance")
    }

    fun requireLeafActive(account: AccountLookupDto) {
        if (!account.accountIsActive) {
            throw RtsGenericException("Account ${account.code} is not active")
        }
        if (account.hasChildren) {
            throw RtsGenericException("Only leaf (postable) accounts can have an opening balance")
        }
    }
}
