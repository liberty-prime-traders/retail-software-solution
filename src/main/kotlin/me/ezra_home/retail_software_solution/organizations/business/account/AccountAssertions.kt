package me.ezra_home.retail_software_solution.organizations.business.account

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountPosting
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

object AccountAssertions {

    fun assertPostable(accounts: Collection<AccountDto>, accountPostings: List<AccountPosting>) {
        val accountsByCode = accounts.associateBy { it.code }
        val parentAccountCodes = parentAccountCodesOf(accounts)
        accountPostings.forEach { accountPosting ->
            val account = requireKnown(accountsByCode, accountPosting.accountCode)
            requireLeaf(account, parentAccountCodes)
            // An inactive account may still be drawn down (reversals, settling old balances), never built up.
            val increasesBalance = accountPosting.entryType == account.accountType.normalBalance
            if (account.accountIsActive.not() && increasesBalance) {
                throw RtsGenericException("Cannot post an increase to inactive account ${account.label}")
            }
        }
    }

    private fun parentAccountCodesOf(accounts: Collection<AccountDto>): Set<String> =
        accounts.mapNotNull { it.parentAccountCode }.toSet()

    private fun requireKnown(accountsByCode: Map<String, AccountDto>, accountCode: String): AccountDto =
        accountsByCode[accountCode] ?: throw RtsGenericException("Cannot post to unknown account $accountCode")

    private fun requireLeaf(account: AccountDto, parentAccountCodes: Set<String>) {
        if (account.code in parentAccountCodes) {
            throw RtsGenericException("Cannot post to ${account.label}: accounts with children never post directly")
        }
    }
}
