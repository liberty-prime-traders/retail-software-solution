package me.ezra_home.retail_software_solution.business.payment_method

import com.google.common.base.Strings
import jakarta.transaction.Transactional
import me.ezra_home.retail_software_solution.business.payment_method.dto.PaymentMethodInsertDto
import me.ezra_home.retail_software_solution.business.payment_method.dto.PaymentMethodResponseDto
import me.ezra_home.retail_software_solution.business.payment_method.dto.PaymentMethodUpdateDto
import me.ezra_home.retail_software_solution.business.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.business.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.business.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Service
class PaymentMethodService (
    private val paymentMethodMapper: PaymentMethodMapper,
    private val paymentMethodCache: PaymentMethodCache
) {

    @Transactional
    fun getAllPaymentMethods(): Collection<PaymentMethodResponseDto> {
        return paymentMethodCache.getAllPaymentMethods().map { paymentMethodMapper.toResponseDto(it) }
    }

    @Transactional
    fun createPaymentMethod(paymentMethodInsertDto: PaymentMethodInsertDto): PaymentMethodResponseDto {
        validateNameOnSave(Optional.ofNullable(paymentMethodInsertDto.name))
        val entity = paymentMethodMapper.toEntity(paymentMethodInsertDto)
        paymentMethodCache.upsertPaymentMethod(entity)
        return paymentMethodMapper.toResponseDto(entity)
    }

    private fun validateNameOnSave(name: Optional<String>?, id: UUID? = null) {
        if (name == null || name.isEmpty || Strings.isNullOrEmpty(name.get())) {
            throw RtsGenericException("A Payment Method must have a name")
        }
        val paymentMethodWithMatchingName = paymentMethodCache.getAllPaymentMethods().find {
            it.name.equals(name.get(), ignoreCase = true) && !Objects.equals(it.id, id)
        }
        if (paymentMethodWithMatchingName != null) {
            throw RtsGenericException("A Payment Method using the name '${name.get()}' already exists")
        }
    }

    @Transactional
    fun updatePaymentMethod(paymentMethodUpdateDto: PaymentMethodUpdateDto): PaymentMethodResponseDto {
        val id = paymentMethodUpdateDto.id ?: throw QueriedByEmptyIdException()
        val entityFromDatabase = paymentMethodCache.getAllPaymentMethods().find { it.id == id } ?: throw UpdatingNonExistingRecordException()
        validateNameOnSave(paymentMethodUpdateDto.name, paymentMethodUpdateDto.id)
        paymentMethodMapper.partialUpdate(paymentMethodUpdateDto, entityFromDatabase)
        paymentMethodCache.upsertPaymentMethod(entityFromDatabase)
        return paymentMethodMapper.toResponseDto(entityFromDatabase)
    }

    @Transactional
    fun deletePaymentMethod(id: UUID?) {
        if (id != null) {
            val entity = paymentMethodCache.getAllPaymentMethods().find { it.id == id }
            if (entity != null) {
                val usageCount = entity.usageCount
                if (usageCount > 0L) {
                    throw RtsGenericException("Payment Method ${entity.name} has $usageCount usage(s) and cannot be deleted")
                }
                paymentMethodCache.deletePaymentMethod(id)
            }

        }
    }
}