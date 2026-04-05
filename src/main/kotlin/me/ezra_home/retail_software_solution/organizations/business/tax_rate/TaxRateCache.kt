package me.ezra_home.retail_software_solution.organizations.business.tax_rate

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.TaxRateEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.TAX_RATE])
internal class TaxRateCache(private val taxRateRepository: TaxRateRepository) {

    @Cacheable
    fun getAll(): Collection<TaxRateEntity> = taxRateRepository.findAll()

    @CacheEvict(allEntries = true)
    fun save(entity: TaxRateEntity) {
        taxRateRepository.save(entity)
    }
}
