package me.ezra_home.retail_software_solution.organizations.business.payment_method

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.organizations.model.PaymentMethodEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.PAYMENT_METHOD])
class PaymentMethodCache (
    private val paymentMethodRepository: PaymentMethodRepository
) {

    @Cacheable
    fun getAllPaymentMethods(): Collection<PaymentMethodEntity> {
        return paymentMethodRepository.findAll()
    }

    @CacheEvict(allEntries = true)
    fun upsertPaymentMethod(paymentMethodEntity: PaymentMethodEntity) {
        paymentMethodRepository.save(paymentMethodEntity)
    }

    @CacheEvict(allEntries = true)
    fun deletePaymentMethod(id: UUID) {
        paymentMethodRepository.deleteById(id)
    }
}
