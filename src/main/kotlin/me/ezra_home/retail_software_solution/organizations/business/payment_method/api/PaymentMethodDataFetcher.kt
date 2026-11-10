package me.ezra_home.retail_software_solution.organizations.business.payment_method.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.PaymentMethodCache
import me.ezra_home.retail_software_solution.organizations.business.payment_method.PaymentMethodMapper
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class PaymentMethodDataFetcher(
    private val paymentMethodMapper: PaymentMethodMapper,
    private val paymentMethodCache: PaymentMethodCache,
    private val accountService: AccountService
) {

    fun getAllPaymentMethods(): Collection<PaymentMethodResponseDto> {
        val accountNamesByCode = accountService.getAccountNamesByCode()
        return paymentMethodCache.getAllPaymentMethods().map {
            paymentMethodMapper.toResponseDto(it, accountNamesByCode[it.accountCode])
        }
    }

    fun findAccountCode(id: UUID): String {
        return findAccountCodesByIds(listOf(id)).getValue(id)
    }

    fun findAccountCodesByIds(ids: Collection<UUID>): Map<UUID, String> {
        val accountCodesById = paymentMethodCache.getAllPaymentMethods().associateBy({ it.id }, { it.accountCode })
        val missingIds = ids.filterNot { it in accountCodesById }
        if (missingIds.isNotEmpty()) {
            throw RtsGenericException("Payment methods not found: ${missingIds.joinToString()}")
        }
        return accountCodesById.filterKeys { it in ids }
    }

    fun getNamesById(): Map<UUID, String> =
        paymentMethodCache.getAllPaymentMethods().mapNotNull { it.name?.let { name -> it.id to name } }.toMap()
}
