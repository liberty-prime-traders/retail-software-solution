package me.ezra_home.retail_software_solution.organizations.business.payment_method

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountUsageProvider
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountUsageType
import org.springframework.stereotype.Component

@Component
class PaymentMethodAccountUsageProvider(
    private val paymentMethodCache: PaymentMethodCache
) : AccountUsageProvider {

    override val usageType = AccountUsageType.PAYMENT_METHOD

    override fun getReferences(accountCode: String): List<String> {
        return paymentMethodCache.getAllPaymentMethods()
            .filter { it.accountCode == accountCode }
            .map { "${it.referenceNumber} - ${it.name}" }
    }
}
