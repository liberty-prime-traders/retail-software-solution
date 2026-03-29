package me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.platform.model.JurisdictionTaxTypeEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.JURISDICTION_TAX_TYPE])
class JurisdictionTaxTypeCache(
    private val jurisdictionTaxTypeRepository: JurisdictionTaxTypeRepository
) {

    @Cacheable
    fun getAll(): Collection<JurisdictionTaxTypeEntity> = jurisdictionTaxTypeRepository.findAll()

    @Cacheable
    fun getActive(): Collection<JurisdictionTaxTypeEntity> = jurisdictionTaxTypeRepository.findAll().filter { it.active }

    @CacheEvict(allEntries = true)
    fun upsertAll(entities: Collection<JurisdictionTaxTypeEntity>) {
        jurisdictionTaxTypeRepository.saveAll(entities)
    }
}
