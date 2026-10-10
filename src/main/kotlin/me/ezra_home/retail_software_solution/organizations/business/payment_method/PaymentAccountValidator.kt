package me.ezra_home.retail_software_solution.organizations.business.payment_method

import me.ezra_home.retail_software_solution.organizations.business.account_selection.api.AccountSelectionRule
import me.ezra_home.retail_software_solution.organizations.business.account_selection.api.AccountSelectionService
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.springframework.stereotype.Component

@Component
class PaymentAccountValidator(private val accountSelectionService: AccountSelectionService) {

    fun validate(accountCode: String) {
        val code = StringUtils.getValueOrException(accountCode, "A Payment Method must have an account")
        accountSelectionService.requireSelectable(AccountSelectionRule.PAYMENT_METHOD, code)
    }
}
