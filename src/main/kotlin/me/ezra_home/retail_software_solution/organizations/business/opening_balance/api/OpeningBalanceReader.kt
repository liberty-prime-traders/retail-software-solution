package me.ezra_home.retail_software_solution.organizations.business.opening_balance.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.opening_balance.OpeningBalanceRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class OpeningBalanceReader(
    private val openingBalanceRepository: OpeningBalanceRepository
) {

    fun getAmountsByAccountCodes(accountCodes: Set<String>): Map<String, BigDecimal> {
        if (accountCodes.isEmpty()) return emptyMap()
        return openingBalanceRepository.findLatestForAccountCodes(accountCodes)
            .associate { it.accountCode to it.amount }
    }
}
