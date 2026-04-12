package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.AccountType
import me.ezra_home.retail_software_solution.organizations.business.account.SystemAccount
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class PaymentAccountValidator(private val accountCache: AccountCache) {

    fun validate(accountCode: String?) {
        val code = StringUtils.getValueOrException(accountCode, "A payment method must be linked to an account")
        val allAccounts = accountCache.getAll()
        val account = allAccounts.firstOrNull { StringUtils.isEquivalent(it.code, code) }
            ?: throw RtsGenericException("Account not found: $code")
        if (account.accountType != AccountType.ASSET) {
            throw RtsGenericException("Payment method account must be of type Asset")
        }

        val parent = allAccounts.firstOrNull { it.id == account.parentAccountId }
            ?: throw RtsGenericException("Payment method account has no parent: $code")
        if (account.accountIsSystemMaintained) {
            val systemAccount = SystemAccount.fromCode(account.code)
            if (systemAccount != SystemAccount.CASH) {
                throw RtsGenericException("Only the Cash system account can be used directly as a payment method account")
            }
        } else if (parent.accountIsSystemMaintained) {
            val parentSystemAccount = SystemAccount.fromCode(parent.code)
            if (parentSystemAccount != SystemAccount.DIGITAL_PAYMENTS) {
                throw RtsGenericException("System-defined payment method account must be a direct child of Digital Payments")
            }
        }
    }
}
