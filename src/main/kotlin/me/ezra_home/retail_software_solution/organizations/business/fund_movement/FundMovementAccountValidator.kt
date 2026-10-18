package me.ezra_home.retail_software_solution.organizations.business.fund_movement

import me.ezra_home.retail_software_solution.organizations.business.account_selection.api.AccountSelectionRule
import me.ezra_home.retail_software_solution.organizations.business.account_selection.api.AccountSelectionService
import org.springframework.stereotype.Component

@Component
class FundMovementAccountValidator(
    private val accountSelectionService: AccountSelectionService
) {

    // Sorted so two movements over the same pair of accounts take the account locks in the same order.
    fun requireSelectable(userPickedAccountCodes: Collection<String>) {
        userPickedAccountCodes.distinct().sorted().forEach { accountCode ->
            accountSelectionService.requireSelectable(AccountSelectionRule.FUND_MOVEMENT, accountCode)
        }
    }
}
