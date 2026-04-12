package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.AccountDto
import me.ezra_home.retail_software_solution.organizations.business.account.AccountType
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
        val allAccounts = accountCache.getAll()
        validatePayableAccountCode(payableAccountCode, allAccounts)
        validateRecoverableAccountCode(recoverableAccountCode, platformTaxType, allAccounts)
    }

    private fun validatePayableAccountCode(code: String?, allAccounts: List<AccountDto>) {
        val normalizedCode = StringUtils.getValueOrException(code, "Every tax type must be associated with a payable account")
        val account = allAccounts.firstOrNull { StringUtils.isEquivalent(it.code, normalizedCode) }
            ?: throw RtsGenericException("Payable account not found: $normalizedCode")
        if (account.accountIsSystemMaintained) {
            val parent = allAccounts.firstOrNull { it.id == account.parentAccountId }
                ?: throw RtsGenericException("Payable account has no parent: $normalizedCode")
            if (SystemAccount.fromCode(parent.code) != SystemAccount.TAX_PAYABLE) {
                throw RtsGenericException("Payable tax account must be under the TAX_PAYABLE parent account")
            }
        } else if (account.accountType != AccountType.LIABILITY) {
            throw RtsGenericException("Org-defined payable tax account must be of type Liability")
        }
    }

    private fun validateRecoverableAccountCode(
        code: String?,
        platformTaxType: PlatformTaxTypeDto,
        allAccounts: List<AccountDto>
    ) {
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

        val account = allAccounts.firstOrNull { StringUtils.isEquivalent(it.code, normalizedCode) }
            ?: throw RtsGenericException("Recoverable account not found: $normalizedCode")
        if (account.accountIsSystemMaintained) {
            val parent = allAccounts.firstOrNull { it.id == account.parentAccountId }
                ?: throw RtsGenericException("Recoverable account has no parent: $normalizedCode")
            if (SystemAccount.fromCode(parent.code) != SystemAccount.TAX_RECOVERABLE) {
                throw RtsGenericException("Recoverable account must be under the TAX_RECOVERABLE parent account")
            }
        } else if (account.accountType != AccountType.ASSET) {
            throw RtsGenericException("Org-defined recoverable tax account must be of type Asset")
        }
    }
}
