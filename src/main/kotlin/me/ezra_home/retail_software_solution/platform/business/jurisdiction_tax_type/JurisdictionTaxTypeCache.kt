package me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_tax_type.api.JurisdictionTaxTypeInsertDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.JURISDICTION_TAX_TYPE])
class JurisdictionTaxTypeCache(
    private val jurisdictionTaxTypeRepository: JurisdictionTaxTypeRepository,
    private val mapper: JurisdictionTaxTypeMapper
) {

    @Cacheable
    fun getAll(): Collection<JurisdictionTaxTypeDto> = jurisdictionTaxTypeRepository.findAll().map { mapper.toDomainDto(it) }

    @Cacheable
    fun getActive(): Collection<JurisdictionTaxTypeDto> = jurisdictionTaxTypeRepository.findAll().filter { it.active }.map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun createAll(insertDtos: Collection<JurisdictionTaxTypeInsertDto>): List<JurisdictionTaxTypeDto> {
        val saved = jurisdictionTaxTypeRepository.saveAllAndFlush(insertDtos.map { mapper.toEntity(it) })
        return saved.map { mapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun saveAll(dtos: Collection<JurisdictionTaxTypeDto>): List<JurisdictionTaxTypeDto> {
        val saved = jurisdictionTaxTypeRepository.saveAll(dtos.map { mapper.toEntity(it) })
        return saved.map { mapper.toDomainDto(it) }
    }
}
