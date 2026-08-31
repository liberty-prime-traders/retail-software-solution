package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.AccountRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema(readOnly = true)
class AccountDataFetcher(
    private val accountRepository: AccountRepository,
    private val accountCache: AccountCache
) {

    fun getByCode(accountCode: String): AccountLookupDto {
        val account = accountCache.getAll().firstOrNull { it.code == accountCode }
            ?: throw RtsGenericException("Account $accountCode does not exist")
        return AccountLookupDto(
            code = account.code,
            accountIsActive = account.accountIsActive,
            hasChildren = accountRepository.existsByParentAccountCode(accountCode),
            normalBalanceEntryType = account.accountType.normalBalance
        )
    }
}
