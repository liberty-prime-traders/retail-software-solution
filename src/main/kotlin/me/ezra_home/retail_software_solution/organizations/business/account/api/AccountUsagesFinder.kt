package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
class AccountUsagesFinder(
    private val referenceProviders: List<AccountUsageProvider>
) {

    fun findUsagesForAccountCode(code: String): List<AccountUsageDto> {
        return referenceProviders
            .map { provider -> AccountUsageDto(provider.usageType, provider.getReferences(code)) }
            .filter { it.references.isNotEmpty() }
    }

    fun failOnUsagesForCode(code: String) {
        val references = findUsagesForAccountCode(code)
        if (references.isNotEmpty()) {
            throw RtsGenericException("Account is in use and cannot have children added to it", references)
        }
    }
}
