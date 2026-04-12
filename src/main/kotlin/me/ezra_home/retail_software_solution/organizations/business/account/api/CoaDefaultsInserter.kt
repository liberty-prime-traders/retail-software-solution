package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.SystemAccount
import org.springframework.stereotype.Component

@Component
@TransactionalOnOrganizationSchema
class CoaDefaultsInserter(private val accountCache: AccountCache) {

    fun seedDefaults() {
        if (accountCache.getAll().isNotEmpty()) return
        accountCache.saveAll(SystemAccount.entries.map { buildInsertDto(it) })
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
