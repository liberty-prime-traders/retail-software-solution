package me.ezra_home.retail_software_solution.organizations.business.payment_method.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountService
import me.ezra_home.retail_software_solution.organizations.business.account.api.PaymentAccountValidator
import me.ezra_home.retail_software_solution.organizations.business.payment_method.PaymentMethodCache
import me.ezra_home.retail_software_solution.organizations.business.payment_method.PaymentMethodMapper
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class PaymentMethodService(
    private val paymentMethodMapper: PaymentMethodMapper,
    private val paymentMethodCache: PaymentMethodCache,
    private val paymentAccountValidator: PaymentAccountValidator,
    private val accountService: AccountService
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllPaymentMethods(): Collection<PaymentMethodResponseDto> {
        val accountNamesByCode = accountService.getAccountNamesByCode()
        return paymentMethodCache.getAllPaymentMethods().map {
            paymentMethodMapper.toResponseDto(it, accountNamesByCode[it.accountCode])
        }
    }

    fun createPaymentMethod(paymentMethodInsertDto: PaymentMethodInsertDto): PaymentMethodResponseDto {
        validateNameOnSave(Optional.ofNullable(paymentMethodInsertDto.name))
        paymentAccountValidator.validate(paymentMethodInsertDto.accountCode)
        val dto = paymentMethodCache.create(paymentMethodInsertDto)
        val accountNamesByCode = accountService.getAccountNamesByCode()
        return paymentMethodMapper.toResponseDto(dto, accountNamesByCode[dto.accountCode])
    }

    private fun validateNameOnSave(optionalName: Optional<String>?, id: UUID? = null) {
        val name = StringUtils.getValueOrException(optionalName, "A Payment Method must have a name")
        paymentMethodCache.getAllPaymentMethods()
            .find { StringUtils.isEquivalent(it.name, name) && !Objects.equals(it.id, id) }
            ?.let { throw RtsGenericException("A Payment Method using the name '$name' already exists") }
    }

    fun updatePaymentMethod(paymentMethodUpdateDto: PaymentMethodUpdateDto): PaymentMethodResponseDto {
        val id = paymentMethodUpdateDto.id ?: throw QueriedByEmptyIdException()
        val existing = paymentMethodCache.getAllPaymentMethods().find { it.id == id } ?: throw UpdatingNonExistingRecordException()
        validateNameOnSave(paymentMethodUpdateDto.name, paymentMethodUpdateDto.id)
        val updated = paymentMethodUpdateDto.applyTo(existing)
        paymentAccountValidator.validate(updated.accountCode)
        val saved = paymentMethodCache.save(updated)
        val accountNamesByCode = accountService.getAccountNamesByCode()
        return paymentMethodMapper.toResponseDto(saved, accountNamesByCode[saved.accountCode])
    }

    fun deletePaymentMethod(id: UUID) {
        paymentMethodCache.deletePaymentMethod(id)
    }
}
