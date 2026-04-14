package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.SystemAccount
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema
class CoaDefaultsInserter(private val accountCache: AccountCache) {

    fun seedDefaults() {
        val allAccounts = accountCache.getAll().map { it.code }.toSet()
        val toInsert = SystemAccount.entries.filter { it.code !in allAccounts }
            .map { buildInsertDto(it) }
        if (toInsert.isNotEmpty()) {
            accountCache.saveAll(toInsert)
        }
    }

    private fun buildInsertDto(account: SystemAccount): AccountInsertDto {
        return AccountInsertDto(
            code = account.code,
            name = account.accountName,
            accountType = account.type,
            accountIsSystemMaintained = true,
            parentAccountCode = account.parent?.code
        )
    }
}
