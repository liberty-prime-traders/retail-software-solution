package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountUsageProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountUsageType
import org.springframework.stereotype.Component

@Component
class FundMovementAccountUsageProvider(
    private val fundMovementRepository: FundMovementRepository
) : AccountUsageProvider {

    override val usageType = AccountUsageType.FUND_MOVEMENT

    override fun getReferences(accountCode: String): List<String> {
        return fundMovementRepository
            .findTop10ByDebitAccountCodeOrCreditAccountCodeOrderByCreatedOnDesc(accountCode, accountCode)
            .map { it.requiredReference() }
    }
}
