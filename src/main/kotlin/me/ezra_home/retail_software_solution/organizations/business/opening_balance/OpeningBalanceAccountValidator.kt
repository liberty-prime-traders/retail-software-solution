package me.ezra_home.retail_software_solution.organizations.business.opening_balance

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountLookupDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

object OpeningBalanceAccountValidator {

    private val subledgerControlAccountCodes = setOf(SystemAccount.TRADE_RECEIVABLES.code, SystemAccount.TRADE_PAYABLES.code)

    fun assertManualOpeningBalanceAccepted(accountCode: String) {
        if (accountCode in subledgerControlAccountCodes) {
            throw RtsGenericException(
                "Account $accountCode is driven by contact opening" +
                    " balances and cannot take a manual opening balance"
            )
        }
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
