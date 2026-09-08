package me.ezra_home.retail_software_solution.organizations.business.opening_balance

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountLookupDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

object OpeningBalanceAccountValidator {

    fun requireLeafActive(account: AccountLookupDto) {
        if (!account.accountIsActive) {
            throw RtsGenericException("Account ${account.code} is not active")
        }
        if (account.hasChildren) {
            throw RtsGenericException("Only leaf (postable) accounts can have an opening balance")
        }
    }
}
