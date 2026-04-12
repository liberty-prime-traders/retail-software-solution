package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.AccountDto
import me.ezra_home.retail_software_solution.organizations.business.account.SystemAccount
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnOrganizationSchema
class CoaDefaultsInserter(private val accountCache: AccountCache) {

    fun seedDefaults() {
        if (accountCache.getAll().isNotEmpty()) return

        val savedAccounts = mutableMapOf<SystemAccount, UUID>()
        var remaining = SystemAccount.entries.toList()

        while (remaining.isNotEmpty()) {
            val batch = remaining.filter { it.parent == null || it.parent in savedAccounts }
            check(batch.isNotEmpty()) {
                "Circular dependency detected among system accounts: ${remaining.joinToString { it.code }}"
            }
            val saved = accountCache.saveAll(batch.map { buildInsertDto(it, savedAccounts) })
            indexSavedIds(batch, saved, savedAccounts)
            remaining = remaining - batch.toSet()
        }
    }

    private fun buildInsertDto(account: SystemAccount, savedIds: Map<SystemAccount, UUID>): AccountInsertDto {
        return AccountInsertDto(
            code = account.code,
            name = account.accountName,
            accountType = account.type,
            accountIsSystemMaintained = true,
            parentAccountId = account.parent?.let { savedIds[it] }
        )
    }

    private fun indexSavedIds(
        batch: List<SystemAccount>,
        saved: List<AccountDto>,
        savedIds: MutableMap<SystemAccount, UUID>
    ) {
        val savedByCode = saved.associateBy { it.code }
        batch.forEach { account ->
            savedIds[account] = savedByCode[account.code]?.id
                ?: throw IllegalStateException("Saved account not found for code: ${account.code}")
        }
    }
}
