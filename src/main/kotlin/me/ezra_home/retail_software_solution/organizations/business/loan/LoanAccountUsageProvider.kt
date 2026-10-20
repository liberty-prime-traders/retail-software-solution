package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountUsageProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountUsageType
import org.springframework.stereotype.Component

@Component
class LoanAccountUsageProvider(
    private val loanRepository: LoanRepository,
    private val loanRepaymentRepository: LoanRepaymentRepository
) : AccountUsageProvider {

    override val usageType = AccountUsageType.LOAN

    override fun getReferences(accountCode: String): List<String> {
        val loanReferences = loanRepository.findTop10ByAssetAccountCodeOrderByCreatedOnDesc(accountCode)
            .map { it.requiredReference() }
        val repaymentReferences = loanRepaymentRepository.findTop10ByAssetAccountCodeOrderByCreatedOnDesc(accountCode)
            .map { it.requiredReference() }
        return loanReferences + repaymentReferences
    }
}
