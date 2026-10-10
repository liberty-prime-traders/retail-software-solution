package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountDto
import me.ezra_home.retail_software_solution.organizations.business.account.AccountType

class AccountSelectionCandidate private constructor(
    account: AccountDto,
    parentAccountCodes: Set<String>,
    private val candidatesByCode: Map<String, AccountSelectionCandidate>
) {
    private val accountType: AccountType = account.accountType

    val code: String = account.code
    val label: String = account.label
    val accountIsActive: Boolean = account.accountIsActive
    val accountIsSystemMaintained: Boolean = account.accountIsSystemMaintained
    val parentAccountCode: String? = account.parentAccountCode
    val systemAccount: SystemAccount? = SystemAccount.fromCode(account.code)
    val isLeaf: Boolean = account.code !in parentAccountCodes
    val parent: AccountSelectionCandidate? by lazy { parentAccountCode?.let { candidatesByCode[it] } }

    fun isAsset(): Boolean = accountType == AccountType.ASSET
    fun isLiability(): Boolean = accountType == AccountType.LIABILITY
    fun isExpense(): Boolean = accountType == AccountType.EXPENSE

    companion object {
        internal fun fromAccounts(accounts: Collection<AccountDto>): List<AccountSelectionCandidate> {
            val parentAccountCodes = accounts.mapNotNull { it.parentAccountCode }.toSet()
            val candidatesByCode = LinkedHashMap<String, AccountSelectionCandidate>()
            accounts.forEach { candidatesByCode[it.code] = AccountSelectionCandidate(it, parentAccountCodes, candidatesByCode) }
            return candidatesByCode.values.toList()
        }
    }
}
