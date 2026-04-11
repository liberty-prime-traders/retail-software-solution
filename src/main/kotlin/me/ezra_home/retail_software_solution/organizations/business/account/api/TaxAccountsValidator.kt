package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.SystemAccount
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.PlatformTaxTypeDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.TaxRecoveryType
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component

@Component
class TaxAccountsValidator(private val accountCache: AccountCache) {

    fun validate(
        payableAccountCode: String?,
        recoverableAccountCode: String?,
        platformTaxType: PlatformTaxTypeDto
    ) {
        validatePayableAccountCode(payableAccountCode)
        validateRecoverableAccountCode(recoverableAccountCode, platformTaxType)
    }

    private fun validatePayableAccountCode(code: String?) {
        val normalizedCode = StringUtils.getValueOrException(code, "Every tax type must be associated with a payable account")
        val allAccounts = accountCache.getAll()
        val account = allAccounts.firstOrNull { StringUtils.isEquivalent(it.code, normalizedCode) }
            ?: throw RtsGenericException("Payable account not found: $normalizedCode")
        val parent = allAccounts.firstOrNull { it.id == account.parentAccountId }
            ?: throw RtsGenericException("Payable account has no parent: $normalizedCode")
        if (SystemAccount.fromCode(parent.code) != SystemAccount.TAX_PAYABLE) {
            throw RtsGenericException("Payable tax account must be under the TAX_PAYABLE parent account")
        }
    }

    private fun validateRecoverableAccountCode(code: String?, platformTaxType: PlatformTaxTypeDto) {
        val normalizedCode = StringUtils.getValueOrNull(code)
        val taxLabel = platformTaxType.label
        if (platformTaxType.taxRecoveryType == TaxRecoveryType.RECOVERABLE) {
            if (normalizedCode == null) {
                throw RtsGenericException("$taxLabel requires a recoverable account")
            }
        } else {
            if (normalizedCode != null) {
                throw RtsGenericException("$taxLabel is not recoverable, so a recoverable account should not be provided")
            }
            return
        }
        val allAccounts = accountCache.getAll()
        val account = allAccounts.firstOrNull { it.code == normalizedCode }
            ?: throw RtsGenericException("Recoverable account not found: $normalizedCode")
        val parent = allAccounts.firstOrNull { it.id == account.parentAccountId }
            ?: throw RtsGenericException("Recoverable account has no parent: $normalizedCode")
        if (SystemAccount.fromCode(parent.code) != SystemAccount.TAX_RECOVERABLE) {
            throw RtsGenericException("Recoverable account must be under the TAX_RECOVERABLE parent account")
        }
    }
}
