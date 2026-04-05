package me.ezra_home.retail_software_solution.organizations.business.payment_method

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.payment_method.dto.PaymentMethodDto
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.PAYMENT_METHOD])
class PaymentMethodCache(
    private val paymentMethodRepository: PaymentMethodRepository,
    private val paymentMethodMapper: PaymentMethodMapper
) {

    @Cacheable
    fun getAllPaymentMethods(): Collection<PaymentMethodDto> {
        return paymentMethodRepository.findAll().map { paymentMethodMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun upsertPaymentMethod(paymentMethodDto: PaymentMethodDto) {
        paymentMethodRepository.save(paymentMethodMapper.toEntity(paymentMethodDto))
    }

    @CacheEvict(allEntries = true)
    fun deletePaymentMethod(id: UUID) {
        paymentMethodRepository.deleteById(id)
    }
}
