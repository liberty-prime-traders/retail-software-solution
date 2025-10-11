package me.ezra_home.retail_software_solution.organizations.business.payment_method

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.payment_method.dto.PaymentMethodInsertDto
import me.ezra_home.retail_software_solution.organizations.business.payment_method.dto.PaymentMethodResponseDto
import me.ezra_home.retail_software_solution.organizations.business.payment_method.dto.PaymentMethodUpdateDto
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
class PaymentMethodService (
    private val paymentMethodMapper: PaymentMethodMapper,
    private val paymentMethodCache: PaymentMethodCache,
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAllPaymentMethods(): Collection<PaymentMethodResponseDto> {
        return paymentMethodCache.getAllPaymentMethods().map { paymentMethodMapper.toResponseDto(it) }
    }

    fun createPaymentMethod(paymentMethodInsertDto: PaymentMethodInsertDto): PaymentMethodResponseDto {
        validateNameOnSave(Optional.ofNullable(paymentMethodInsertDto.name))
        val entity = paymentMethodMapper.toEntity(paymentMethodInsertDto)
        paymentMethodCache.upsertPaymentMethod(entity)
        return paymentMethodMapper.toResponseDto(entity)
    }

    private fun validateNameOnSave(optionalName: Optional<String>?, id: UUID? = null) {
        val name = StringUtils.getValueOrException(optionalName, "A Payment Method must have a name")
        paymentMethodCache.getAllPaymentMethods()
            .find { StringUtils.isEquivalent(it.name, name) && !Objects.equals(it.id, id) }
            ?.let { throw RtsGenericException("A Payment Method using the name '$name' already exists") }
    }

    fun updatePaymentMethod(paymentMethodUpdateDto: PaymentMethodUpdateDto): PaymentMethodResponseDto {
        val id = paymentMethodUpdateDto.id ?: throw QueriedByEmptyIdException()
        val entityFromDatabase = paymentMethodCache.getAllPaymentMethods().find { it.id == id } ?: throw UpdatingNonExistingRecordException()
        validateNameOnSave(paymentMethodUpdateDto.name, paymentMethodUpdateDto.id)
        paymentMethodMapper.partialUpdate(paymentMethodUpdateDto, entityFromDatabase)
        paymentMethodCache.upsertPaymentMethod(entityFromDatabase)
        return paymentMethodMapper.toResponseDto(entityFromDatabase)
    }

    fun deletePaymentMethod(id: UUID?) {
        id?.let {
            paymentMethodCache.getAllPaymentMethods().find { it.id == id }?.let { entity ->
                val usageCount = entity.usageCount
                if (usageCount > 0L) {
                    throw RtsGenericException("Payment Method ${entity.name} has $usageCount usage(s) and cannot be deleted")
                }
                paymentMethodCache.deletePaymentMethod(id)
            }
        }
    }
}
