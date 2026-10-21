package me.ezra_home.retail_software_solution.organizations.business.loan

import me.ezra_home.retail_software_solution.organizations.business.account_selection.api.AccountSelectionRule
import me.ezra_home.retail_software_solution.organizations.business.account_selection.api.AccountSelectionService
import org.springframework.stereotype.Component

@Component
class LoanAccountValidator(
    private val accountSelectionService: AccountSelectionService
) {

    fun requireSelectable(assetAccountCode: String) {
        accountSelectionService.requireSelectable(AccountSelectionRule.LOAN, assetAccountCode)
    }
}
